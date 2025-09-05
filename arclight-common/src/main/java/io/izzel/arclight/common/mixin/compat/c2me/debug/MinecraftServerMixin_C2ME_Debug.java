package io.izzel.arclight.common.mixin.compat.c2me.debug;

import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfProperty;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.apache.commons.lang3.mutable.MutableLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@LoadIfProperty("arclight.c2me.debug")
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_C2ME_Debug extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerBridge {
    public MinecraftServerMixin_C2ME_Debug(String string) {
        super(string);
    }

    @Shadow protected abstract boolean haveTime();

    @Redirect(method = "managedBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantBlockableEventLoop;managedBlock(Ljava/util/function/BooleanSupplier;)V"))
    private void arclight$onManagedBlockDecoration(ReentrantBlockableEventLoop<TickTask> instance, BooleanSupplier booleanSupplier) {
        MutableLong timer = new MutableLong(Util.getMillis());
        Exception ex = new Exception("managedBlock stack trace");
        super.managedBlock(() -> {
            if (Util.getMillis() - timer.longValue() > 5000L) {
                ArclightServer.LOGGER.warn("Blocking for more than 5 seconds, resetting timer; haveTime(): {}", haveTime(), ex);
                timer.setValue(Util.getMillis());
            }
            return booleanSupplier.getAsBoolean();
        });
    }

    @Inject(method = "pollTask", at = @At("HEAD"))
    private void arclight$pollTask(CallbackInfoReturnable<Boolean> cir) {
        ArclightServer.LOGGER.warn("Polling task. haveTime? {} runningTask? {}", haveTime(), runningTask(), new Exception());
    }

    @Inject(method = "waitUntilNextTick", at = @At("RETURN"))
    private void arclight$finishWait(CallbackInfo ci) {
        ArclightServer.LOGGER.warn("Waiting for next tick finished");
    }
}
