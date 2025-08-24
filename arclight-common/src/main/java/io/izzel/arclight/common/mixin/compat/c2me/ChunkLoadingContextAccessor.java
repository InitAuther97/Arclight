package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.Coerce;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(targets = "com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext")
public interface ChunkLoadingContextAccessor {

    @Coerce
    @Accessor("holder")
    ItemHolderInvoker<ChunkPos, ? extends ChunkStateAccessor, ? extends ChunkLoadingContextAccessor, ? extends ChunkHolder> arclight$getItemHolder();

    @Accessor("tacs")
    ChunkMap arclight$getChunkMap();
}
