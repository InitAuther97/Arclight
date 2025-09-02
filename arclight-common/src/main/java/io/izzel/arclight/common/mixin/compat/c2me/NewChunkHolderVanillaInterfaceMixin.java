package io.izzel.arclight.common.mixin.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
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
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(NewChunkHolderVanillaInterface.class)
public abstract class NewChunkHolderVanillaInterfaceMixin extends ChunkHolderMixin_C2ME implements NewChunkHolderVanillaInterfaceBridge {

    @Shadow @Final private ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> newHolder;

    protected NewChunkHolderVanillaInterfaceMixin(ChunkPos chunkPos) {
        super(chunkPos);
    }

    private BooleanSupplier canceller;
    private CompletableFuture<Void> result;

    @Override
    public CompletableFuture<Void> arclight$scheduleUnload(ChunkLoadingContext ctx) {
        LevelChunk chunk = (LevelChunk) newHolder.getItem().get().chunk();
        Assertions.assertTrue(result.isDone(), "BUG: Scheduling unload before event future is done");
        if (result.isCompletedExceptionally()) {
            return result = CompletableFuture.completedFuture(null);
        } else {
            return result = (CompletableFuture<Void>) Completable.create(emitter -> {
                        this.canceller = () -> emitter.tryOnError(C2MEScope.CANCELLED);
                        C2MEScope.MAIN_THREAD_MAILBOX.executeIfPossible(emitter::onComplete);
                    })
                    .andThen(Completable.create(emitter -> ((ItemHolderBridge) newHolder).arclight$runBusyNow(emitter, ((ChunkBridge) chunk)::bridge$unloadCallback))
                            .subscribeOn(C2MEScope.SCHEDULER_BACKED_BY_SERVER))
                    .<Void>toCompletionStage(null);
        }
    }

    @Override
    public void arclight$scheduleLoad(ChunkLoadingContext ctx) {
        Assertions.assertTrue(result == null || result.isDone(), "BUG: Scheduling load before event future is done");
        canceller = null;
        result = (CompletableFuture<Void>) Completable.fromCompletionStage(newHolder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE))
                .andThen(Completable.create(emitter -> {
                            ((ItemHolderBridge) newHolder).arclight$runBusyNow(emitter, () -> {
                                ChunkState state = newHolder.getItem().get();
                                Assertions.assertTrue(newHolder.isOpen() && state.chunk() instanceof LevelChunk, "BUG: Chunk load event is not cancelled before chunk unloading");
                                ((ChunkBridge) state.chunk()).bridge$loadCallback();
                            });
                        })
                        .subscribeOn(C2MEScope.SCHEDULER_BACKED_BY_SERVER))
                .<Void>toCompletionStage(null);
    }

    @Override
    public void arclight$cancelIfNecessary(boolean isUpgrade) {
        if (canceller != null && isUpgrade) {
            canceller.getAsBoolean();
        }
    }
}
