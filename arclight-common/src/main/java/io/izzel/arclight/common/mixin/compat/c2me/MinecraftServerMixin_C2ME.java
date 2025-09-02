package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.mod.compat.c2me.C2MEScope;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_C2ME {
    @Inject(method = "pollTaskInternal", at = @At(value = "RETURN", ordinal = 2), cancellable = true)
    private void arclight$pollChunkEvent(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(C2MEScope.MAIN_THREAD_MAILBOX.pollTask());
    }
}
