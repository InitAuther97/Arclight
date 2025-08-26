package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.mod.compat.C2MECompat;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(targets = "com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessible", remap = false)
public class ServerAccessibleMixin {

    @Decorate(method = "lambda$upgradeToThis$0", at = @At("TAIL"), inject = true)
    private static void arclight$afterChunkAccessible(@Local(ordinal = -1) LevelChunk fullChunk) {
        C2MECompat.callChunkLoad(fullChunk);
    }

    @Inject(method = "lambda$downgradeFromThis$3", at = @At("HEAD"))
    private static void arclight$beforeChunkInaccessible(@Coerce Object ctx, ChunkAccess chunk, @Coerce Object chunkState, CallbackInfo ci) {
        C2MECompat.callChunkUnload((LevelChunk) chunk);
    }
}