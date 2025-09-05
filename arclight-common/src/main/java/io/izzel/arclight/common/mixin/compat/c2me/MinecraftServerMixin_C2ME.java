package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.compat.c2me.C2MEScope;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_C2ME extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerBridge {
    public MinecraftServerMixin_C2ME(String string) {
        super(string);
    }

    @Shadow protected abstract boolean haveTime();

    @Inject(method = "pollTaskInternal", at = @At(value = "RETURN", ordinal = 2), cancellable = true)
    private void arclight$pollChunkEvent(CallbackInfoReturnable<Boolean> cir) {
        if (arclight$haveTime()) {
            // Since Mojang attempts to poll out everything first in their waitForTasks, it is key that we don't poll if there's no time.
            // Otherwise, a cancelled load event may lead to a deadlock.
            cir.setReturnValue(C2MEScope.MAIN_THREAD_MAILBOX.pollTask());
        }
    }

    @Inject(method = "waitForTasks", at = @At("HEAD"), cancellable = true)
    private void arclight$skipWaitIfNoTime(CallbackInfo ci) {
        if (!haveTime()) {
            Thread.yield();
            ci.cancel();
        }
    }
}
