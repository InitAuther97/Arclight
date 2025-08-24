package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.Coerce;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(targets = "com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus")
public interface NewChunkStatusAccessor {

    @Coerce
    @Accessor("SERVER_ACCESSIBLE")
    static NewChunkStatusAccessor arclight$serverAccessible() {
        throw new AbstractMethodError();
    }
}
