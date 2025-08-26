package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_C2ME implements ServerWorldBridge {

    @Shadow @Final private ServerChunkCache chunkSource;

    @Override
    public void arclight$setChunkEvent(long pos, LevelChunk unloading) {
        ((ServerChunkProviderBridge) this.chunkSource).arclight$setChunkEvent(pos, unloading);
    }
}
