package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(targets = "com.ishland.c2me.rewrites.chunksystem.common.ChunkState")
public interface ChunkStateAccessor {

    @Accessor("chunk")
    ChunkAccess arclight$getChunk();
}
