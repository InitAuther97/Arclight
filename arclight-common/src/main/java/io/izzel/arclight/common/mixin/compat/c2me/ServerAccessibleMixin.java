package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(targets = "com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessible")
public class ServerAccessibleMixin {

    @Coerce
    @Inject(method = "lambda$upgradeToThis$0", at = @At("TAIL"))
    private void arclight$afterChunkAccessible(ChunkLoadingContextAccessor ctx, ProtoChunk protoChunk, CallbackInfo ci) {
        var itemHolder = ctx.arclight$getItemHolder();
        ChunkMap map = ctx.arclight$getChunkMap();
        ChunkBridge fullChunk = (ChunkBridge) itemHolder.arclight$getItem().get().arclight$getChunk();
        itemHolder.arclight$getFutureForStatusSafely(NewChunkStatusAccessor.arclight$serverAccessible())
                .thenAccept(unused -> ((ChunkMapBridge) map).arclight$addCallback(fullChunk::bridge$loadCallback));
    }

    @Coerce
    @Inject(method = "lambda$downgradeFromThis$3", at = @At("HEAD"))
    private void arclight$beforeChunkInaccessible(ChunkLoadingContextAccessor ctx, ChunkAccess chunk, ChunkStateAccessor chunkState, CallbackInfo ci) {
        ChunkBridge fullChunk = (ChunkBridge) chunk;
        fullChunk.bridge$unloadCallback();
    }
}
