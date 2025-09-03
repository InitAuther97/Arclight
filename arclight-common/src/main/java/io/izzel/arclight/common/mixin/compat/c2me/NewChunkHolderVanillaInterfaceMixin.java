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
    private CompletableFuture<Void> loadFuture;
    private CompletableFuture<Void> unloadFuture;

    @Override
    public CompletableFuture<Void> arclight$scheduleUnload(ChunkLoadingContext ctx, Cancellable cancellable) {
        LevelChunk chunk = (LevelChunk) newHolder.getItem().get().chunk();
        canceller.setRelease(null);
        if (loadFuture != null && !loadFuture.isCompletedExceptionally()) { // either not done or successfully completed
            return unloadFuture = (CompletableFuture<Void>) Completable.fromCompletionStage(loadFuture) // need to wait until load event is sent; on main thread or scheduler
                    .andThen(Completable.create(emitter -> {
                            if (canceller.compareAndExchange(null, () -> emitter.tryOnError(C2MEScope.CANCELLED)) != null) { // check for early stop, chunk is loading again before it can finish unloading
                                // cancel unloading
                                emitter.tryOnError(C2MEScope.CANCELLED);
                            }
                            // perform unload event when server is ready
                            C2MEScope.MAIN_THREAD_MAILBOX.executeIfPossible(emitter::onComplete);
                        })
                        .andThen(Completable.create(emitter -> ((ItemHolderBridge) newHolder).arclight$runBusyNow(emitter, () -> true, // perform unload event
                                        ((ChunkBridge) chunk)::bridge$unloadCallback))
                                .subscribeOn(C2MEScope.SCHEDULER_BACKED_BY_SERVER))) // ensure it's running on server
                    .doOnError(th -> cancellable.cancel()) // don't forget to cancel downgrading if it's completed with CANCELLED
                    .<Void>toCompletionStage(null); // do unload after unload event
        }
        return unloadFuture = CompletableFuture.completedFuture(null); // load event is cancelled before it can be sent, do unload w/o unload event
    }

    @Override
    public void arclight$scheduleLoad(ChunkLoadingContext ctx) {
        Assertions.assertTrue(loadFuture == null || loadFuture.isDone(), "BUG: Scheduling load before event future is done");
        canceller.setRelease(null);
        loadFuture = (CompletableFuture<Void>) Completable.fromCompletionStage(newHolder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE)) // completed when doOnEvent, still busy
                .andThen(Completable.create(emitter -> {
                            ((ItemHolderBridge) newHolder).arclight$runBusyNow(emitter, () -> { // on main thread, after indefinite time
                                final var future = newHolder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE);
                                return future.isDone() && !future.isCompletedExceptionally(); // check for ongoing downgrade from FULL; if any stop immediately
                            }, () -> {
                                // perform load event
                                ChunkState state = newHolder.getItem().get();
                                Assertions.assertTrue(newHolder.isOpen() && state.chunk() instanceof LevelChunk, "BUG: Chunk load event is not cancelled before chunk unloading");
                                ((ChunkBridge) state.chunk()).bridge$loadCallback();
                            });
                        })
                        .subscribeOn(C2MEScope.SCHEDULER_BACKED_BY_SERVER)) // subscribe on main thread, doOnEvent complete() finish here
                .<Void>toCompletionStage(null);
    }

    @Override
    public void arclight$cancelIfNecessary(boolean isUpgrade) {
        if (isUpgrade) {
            loadFuture.thenRun(() -> {
                BooleanSupplier canceller = this.canceller.compareAndExchange(null, C2MEScope.TRUE);
                if (canceller != null) canceller.getAsBoolean();
            });
        }
    }
}
