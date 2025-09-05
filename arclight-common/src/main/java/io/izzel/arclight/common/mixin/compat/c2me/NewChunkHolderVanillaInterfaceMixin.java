package io.izzel.arclight.common.mixin.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.util.Assertions;
import io.izzel.arclight.common.bridge.compat.c2me.ItemHolderBridge;
import io.izzel.arclight.common.bridge.compat.c2me.NewChunkHolderVanillaInterfaceBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.izzel.arclight.common.mod.compat.c2me.C2MEScope;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(value = NewChunkHolderVanillaInterface.class, remap = false)
public abstract class NewChunkHolderVanillaInterfaceMixin extends ChunkHolderMixin_C2ME implements NewChunkHolderVanillaInterfaceBridge {

    @Shadow @Final private ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> newHolder;

    @Shadow public abstract ChunkPos getPos();

    protected NewChunkHolderVanillaInterfaceMixin(ChunkPos chunkPos) {
        super(chunkPos);
    }

    private final AtomicReference<BooleanSupplier> canceller = new AtomicReference<>();
    private Completable loadFuture;
    private Disposable loadAction;
    private CompletableFuture<Void> unloadFuture;

    @Override
    public CompletableFuture<Void> arclight$scheduleUnload(ChunkLoadingContext ctx, Cancellable cancellable) {
        // A cancellation is already sent by setStatus which cancels the future for ACCESSIBLE
        LevelChunk chunk = (LevelChunk) newHolder.getItem().get().chunk();
        canceller.setRelease(null);
        if (loadFuture != null) {
            return unloadFuture = (CompletableFuture<Void>) loadFuture.andThen( // need to wait until load event is sent; completed on main thread or cancelled on scheduler
                            Completable.create(emitter -> {
                                // check for early stop, chunk is loading again before it can finish unloading
                                if (canceller.compareAndExchange(null, () -> emitter.tryOnError(C2MEScope.UNLOAD_CANCELLED)) != null) {
                                    // cancel unloading
                                    emitter.tryOnError(C2MEScope.UNLOAD_CANCELLED);
                                }
                                // perform unload event when server is ready, this is reentrant which will not enter during another chunk event
                                C2MEScope.SCHEDULER_BACKED_BY_SERVER_REENTRANT.scheduleDirect(emitter::onComplete);
                            }).andThen(Completable.create(emitter -> {
                                // We're mutual exclusively busy naturally because unload won't proceed. Only busy ticking will occur now, perform unload event
                                ((ItemHolderBridge) newHolder).arclight$runBusyNow(emitter, ((ChunkBridge) chunk)::bridge$unloadCallback);
                            }).subscribeOn(C2MEScope.SCHEDULER_BACKED_BY_SERVER)) // ensure it's running on server, this is not reentrant which may fall through if already on server
                    )
                    .onErrorComplete(it -> it == C2MEScope.LOAD_EVENT_CANCELLED) // if load event failed then proceed to unload.
                    .doOnError(th -> cancellable.cancel()) // don't forget to cancel downgrading if it's failed
                    .<Void>toCompletionStage(null); // do unload after unload event
        }
        return unloadFuture = CompletableFuture.completedFuture(null); // load event is cancelled before it can be sent, do unload w/o unload event
    }

    @Override
    public void arclight$scheduleLoad(ChunkLoadingContext ctx) {
        Assertions.assertTrue(
                (unloadFuture == null || unloadFuture.isDone()) && (loadFuture == null || loadAction.isDisposed()),
                "BUG: Scheduling load before event future is done"
        );
        MinecraftServer server = ctx.tacs().level.getServer();
        MinecraftServerBridge bridge = (MinecraftServerBridge) server;
        canceller.setRelease(null);
        loadFuture = Completable.fromCompletionStage(newHolder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE)) // completed when doOnEvent setStatus on main thread (possibly), still busy
                .andThen(Completable.create(emitter -> {
                            // Fast check in advance for retry: if it's already cancelled then we fail immediately
                            final var future0 = newHolder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE);
                            if (!future0.isDone() || future0.isCompletedExceptionally()) {
                                emitter.onError(C2MEScope.LOAD_EVENT_CANCELLED);
                            }
                            // Chunk event mailbox won't be reentrant polled because no task will be polled when it's running.
                            // So it's safe (need extra care still) to poll the whole server for tasks.
                            ((ItemHolderBridge) newHolder).arclight$runBusyNow(server, emitter, () -> { // on main thread, after indefinite time
                                // if (!newHolder.isOpen()) return C2MEScope.LOAD_EVENT_CANCELLED; // No need for now since unload will not proceed before load event is completed.
                                if (!bridge.arclight$haveTime()) {
                                    // We may run out of tick time before we can proceed. In that case no task can be polled and there will be deadlock. So if tick time runs out fail immediately.
                                    return C2MEScope.MUTEX_TIMEOUT;
                                }
                                // check for ongoing downgrade from FULL; if any stop immediately
                                final var future = newHolder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE);
                                if (future.isDone() && !future.isCompletedExceptionally()) {
                                    return null;
                                }
                                // ArclightCaptures.captureWaitingForChunk(null, null);
                                return C2MEScope.LOAD_EVENT_CANCELLED;
                            }, () -> {
                                // We're mutual exclusively busy and only busy ticking will occur now, perform load
                                ChunkState state = newHolder.getItem().get();
                                Assertions.assertTrue(newHolder.isOpen() && state.chunk() instanceof LevelChunk, "BUG: Chunk load event is not cancelled before chunk unloading");
                                ((ChunkBridge) state.chunk()).bridge$loadCallback();
                            });
                        }).subscribeOn(C2MEScope.SCHEDULER_BACKED_BY_SERVER_TELL)
                        // impossible to short circuit because we need to release advanceStatus op but there's a chance setStatus won't run on main thread.
                        // so it's unsure whether the ref count should be 1 or 2 if we fall through.
                        .retry(it -> it == C2MEScope.MUTEX_TIMEOUT)) // If failed to proceed due to timeout, retry
                .cache();
        loadAction = loadFuture.onErrorComplete().subscribe();
    }

    @Override
    public void arclight$cancelIfNecessary(boolean isUpgrade) {
        if (isUpgrade) {
            loadFuture.subscribe(() -> {
                BooleanSupplier canceller = this.canceller.compareAndExchange(null, C2MEScope.TRUE);
                if (canceller != null) canceller.getAsBoolean();
            }, unused -> {});
        }
    }
}
