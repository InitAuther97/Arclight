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
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.izzel.arclight.common.mod.compat.c2me.C2MEScope;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.reactivex.rxjava3.core.Completable;
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

    protected NewChunkHolderVanillaInterfaceMixin(ChunkPos chunkPos) {
        super(chunkPos);
    }

    private final AtomicReference<BooleanSupplier> canceller = new AtomicReference<>();
    private Completable loadFuture;
    private CompletableFuture<Void> unloadFuture;

    @Override
    public CompletableFuture<Void> arclight$scheduleUnload(ChunkLoadingContext ctx, Cancellable cancellable) {
        // A cancellation is already sent by setStatus which cancels the future for ACCESSIBLE
        LevelChunk chunk = (LevelChunk) newHolder.getItem().get().chunk();
        MinecraftServer server = ctx.tacs().level.getServer();
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
                                // We're mutually busy and only busy ticking will occur now, perform unload event
                                ((ItemHolderBridge) newHolder).arclight$runBusyNow(server, emitter, () -> null, ((ChunkBridge) chunk)::bridge$unloadCallback);
                            }).subscribeOn(C2MEScope.SCHEDULER_BACKED_BY_SERVER)) // ensure it's running on server, this is not reentrant which may fall through if already on server
                    )
                    .onErrorComplete(it -> it == C2MEScope.LOAD_EVENT_CANCELLED) // if it's failed then no need to unload; if load event failed then proceed to unload.
                    .doOnError(th -> cancellable.cancel()) // don't forget to cancel downgrading if it's failed
                    .<Void>toCompletionStage(null); // do unload after unload event
        }
        return unloadFuture = CompletableFuture.completedFuture(null); // load event is cancelled before it can be sent, do unload w/o unload event
    }

    @Override
    public void arclight$scheduleLoad(ChunkLoadingContext ctx) {
        Assertions.assertTrue(
                (unloadFuture == null || unloadFuture.isDone()) && (loadFuture == null || loadFuture.onErrorComplete().subscribe().isDisposed()),
                "BUG: Scheduling load before event future is done"
        );
        MinecraftServer server = ctx.tacs().level.getServer();
        canceller.setRelease(null);
        loadFuture = Completable.fromCompletionStage(newHolder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE)) // completed when doOnEvent setStatus on main thread, still busy
                .andThen(Completable.create(emitter -> {
                            ((ItemHolderBridge) newHolder).arclight$runBusyNow(server, emitter, () -> { // on main thread, after indefinite time
                                // if (!newHolder.isOpen()) return C2MEScope.LOAD_EVENT_CANCELLED; // No need for now since unload will not proceed before load event is completed
                                // check for ongoing downgrade from FULL; if any stop immediately
                                final var future = newHolder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE);
                                return future.isDone() && !future.isCompletedExceptionally() ? null : C2MEScope.LOAD_EVENT_CANCELLED;
                            }, () -> {
                                // We're mutually busy and only busy ticking will occur now, perform load
                                ChunkState state = newHolder.getItem().get();
                                Assertions.assertTrue(newHolder.isOpen() && state.chunk() instanceof LevelChunk, "BUG: Chunk load event is not cancelled before chunk unloading");
                                ((ChunkBridge) state.chunk()).bridge$loadCallback();
                            });
                        }).subscribeOn(C2MEScope.SCHEDULER_BACKED_BY_SERVER_REENTRANT) // subscribe on main thread, can fall through on depth 0
                ).cache();
        loadFuture.onErrorComplete().subscribe();
    }

    @Override
    public void arclight$cancelIfNecessary(boolean isUpgrade) {
        if (isUpgrade) {
            loadFuture.doOnComplete(() -> {
                BooleanSupplier canceller = this.canceller.compareAndExchange(null, C2MEScope.TRUE);
                if (canceller != null) canceller.getAsBoolean();
            }).subscribe();
        }
    }
}
