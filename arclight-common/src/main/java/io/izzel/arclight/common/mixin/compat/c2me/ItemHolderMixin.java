package io.izzel.arclight.common.mixin.compat.c2me;

import com.ishland.flowsched.scheduler.BusyRefCounter;
import com.ishland.flowsched.scheduler.ItemHolder;
import io.izzel.arclight.common.bridge.compat.c2me.ItemHolderBridge;
import io.izzel.arclight.common.mod.compat.c2me.C2MEScope;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.reactivex.rxjava3.core.CompletableEmitter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(ItemHolder.class)
public abstract class ItemHolderMixin implements ItemHolderBridge {

    @Shadow public abstract boolean isOpen();

    @Shadow @Final private BusyRefCounter busyRefCounter;

    @Override
    public void arclight$runBusyNow(CompletableEmitter emitter, Runnable runnable) {
        synchronized (this) {
            if (!isOpen()) {
                emitter.onError(C2MEScope.CANCELLED);
                return;
            }
            busyRefCounter.incrementRefCount();
        }
        try {
            runnable.run();
        } finally {
            busyRefCounter.decrementRefCount();
            emitter.onComplete();
        }
    }
}
