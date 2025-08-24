package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.world.IllegalChunkAccessException;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin_C2ME implements ServerChunkProviderBridge {

    // @formatter:off
    @Shadow @Final ServerLevel level;
    // @formatter:on

    @Unique
    private ChunkPos arclight$currentChunkEvent = null;

    @Unique
    private LevelChunk arclight$currentUnloading = null;

    @Inject(method = "getChunkFutureMainThread", at = @At("HEAD"))
    private void arclight$shortcutGetChunk(int chunkX, int chunkZ, ChunkStatus status, boolean generate, CallbackInfoReturnable<LevelChunk> cir) {
        if (this.arclight$currentChunkEvent != null) {
            if (this.arclight$currentChunkEvent.x != chunkX || this.arclight$currentChunkEvent.z != chunkZ) {
                if (generate) {
                    RuntimeException ex = new IllegalChunkAccessException(this.level.dimension.location(), arclight$currentChunkEvent, new ChunkPos(chunkX, chunkZ));
                    ArclightServer.LOGGER.error("Detected an illegal chunk request during chunk event. This will lead to a dangerous undefined behaviour! Report this to the author of the respective plugin or mod!", ex);
                    throw ex;
                }
            } else if (this.arclight$currentUnloading != null) {
                cir.setReturnValue(this.arclight$currentUnloading);
            }
        }
    }

    @Override
    public void arclight$setUnloadingChunk(ChunkPos pos, LevelChunk unloading) {
        arclight$currentChunkEvent = pos;
        arclight$currentUnloading = unloading;
    }
}
