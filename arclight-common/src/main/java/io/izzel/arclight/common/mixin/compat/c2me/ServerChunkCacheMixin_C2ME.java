package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin_C2ME {

    /**
     * @author InitAuther97
     * @reason Use C2ME chunk system
     */
    @Dynamic
    @Overwrite
    public boolean isChunkLoaded(int x, int z) {
        return false;
    }

    /**
     * @author InitAuther97
     * @reason Use C2ME chunk system
     */
    @Dynamic
    @Overwrite
    public LevelChunk getChunkUnchecked(int chunkX, int chunkZ) {
        return null;
    }
}
