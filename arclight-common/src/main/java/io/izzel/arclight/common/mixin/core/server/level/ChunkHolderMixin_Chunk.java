package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.ABSENT)
@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin_Chunk extends GenerationChunkHolder implements ChunkHolderBridge {

    // @formatter:off
    @Shadow public int oldTicketLevel;
    @Shadow private int ticketLevel;
    @Shadow public abstract CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture();
    @Override @Accessor("oldTicketLevel") public abstract int bridge$getOldTicketLevel();
    // @formatter:on

    @Unique
    private boolean arclight$pendingUnload;

    public ChunkHolderMixin_Chunk(ChunkPos chunkPos) {
        super(chunkPos);
    }

    public LevelChunk getFullChunkNow() {
        if (!ChunkLevel.fullStatus(this.oldTicketLevel).isOrAfter(FullChunkStatus.FULL)) return null;
        return this.getFullChunkNowUnchecked();
    }

    public LevelChunk getFullChunkNowUnchecked() {
        return (LevelChunk) this.getChunkIfPresentUnchecked(ChunkStatus.FULL);
    }

    @Override
    public LevelChunk bridge$getFullChunkNow() {
        return this.getFullChunkNow();
    }

    @Override
    public LevelChunk bridge$getFullChunkUnchecked() {
        return this.getFullChunkNowUnchecked();
    }

    @Override
    public void bridge$callEventIfUnloading(ChunkMap manager) {
        callEventIfUnloading(manager);
    }

    @Override
    public boolean arclight$isCurrentlyUnloading() {
        FullChunkStatus old = ChunkLevel.fullStatus(oldTicketLevel);
        FullChunkStatus current = ChunkLevel.fullStatus(ticketLevel);
        return old.isOrAfter(FullChunkStatus.FULL) && !current.isOrAfter(FullChunkStatus.FULL);
    }

    protected void callEventIfUnloading(ChunkMap manager) {
        if (arclight$isCurrentlyUnloading()) {
            if (arclight$pendingUnload) {
                return;
            }
            arclight$pendingUnload = true;
            this.getFullChunkFuture().thenAccept((either) -> {
                LevelChunk chunk = either.orElse(null);
                if (chunk != null) {
                    ((ChunkMapBridge)manager).arclight$addCallback(() -> {
                        // Minecraft will apply the chunks tick lists to the world once the chunk got loaded, and then store the tick
                        // lists again inside the chunk once the chunk becomes inaccessible and set the chunk's needsSaving flag.
                        // These actions may however happen deferred, so we manually set the needsSaving flag already here.
                        chunk.setUnsaved(true);
                        ((ChunkBridge)chunk).bridge$unloadCallback();
                    });
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                MinecraftServer.LOGGER.error("Failed to schedule unload callback for chunk " + pos, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            ((ChunkMapBridge) manager).arclight$runCallbacks();
        } else if (arclight$pendingUnload) {
            ArclightServer.LOGGER.warn("Chunk status changed multiple times during chunk ticket tracking / unload event for chunk {}", getPos());
        }
    }

    protected void callEventIfLoading(ChunkMap manager) {
        arclight$pendingUnload = false;
        FullChunkStatus fullChunkStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus fullChunkStatus2 = ChunkLevel.fullStatus(this.ticketLevel);
        this.oldTicketLevel = this.ticketLevel;
        if (!fullChunkStatus.isOrAfter(FullChunkStatus.FULL) && fullChunkStatus2.isOrAfter(FullChunkStatus.FULL)) {
            this.getFullChunkFuture().thenAccept((either) -> {
                LevelChunk chunk = either.orElse(null);
                if (chunk != null) {
                    ((ChunkMapBridge) manager).arclight$addCallback(
                            ((ChunkBridge) chunk)::bridge$loadCallback
                    );
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                ArclightServer.LOGGER.fatal("Failed to schedule load callback for chunk " + this.pos, throwable);
                return null;
            });

            ((ChunkMapBridge) manager).arclight$runCallbacks();
        }
    }

    // Note that this logic is slightly different from the one above
    @Inject(method = "updateFutures", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ChunkHolder$LevelChangeListener;onLevelChange(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/IntSupplier;ILjava/util/function/IntConsumer;)V"))
    private void arclight$onChunkLoad(ChunkMap chunkManager, Executor executor, CallbackInfo ci) {
        callEventIfLoading(chunkManager);
    }
}
