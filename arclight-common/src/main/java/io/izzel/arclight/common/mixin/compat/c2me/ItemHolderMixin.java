package io.izzel.arclight.common.mixin.compat.c2me;

import com.ishland.flowsched.scheduler.BusyRefCounter;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.util.Assertions;
import io.izzel.arclight.common.bridge.compat.c2me.BlockableEventLoopBridge_C2ME;
import io.izzel.arclight.common.bridge.compat.c2me.BusyRefCounterBridge;
import io.izzel.arclight.common.bridge.compat.c2me.ItemHolderBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.reactivex.rxjava3.core.CompletableEmitter;
import net.minecraft.util.thread.BlockableEventLoop;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(value = ItemHolder.class, remap = false)
public abstract class ItemHolderMixin implements ItemHolderBridge {

    @Shadow @Final private BusyRefCounter busyRefCounter;

    @Shadow public abstract boolean isOpen();

    @Override
    public void arclight$runBusyNow(BlockableEventLoop<?> blocker, CompletableEmitter emitter, Supplier<Throwable> condition, Runnable runnable) {
        Assertions.assertTrue(blocker.isSameThread(), "BUG: trying to managedBlock off the running thread of the BlockableEventLoop");
        MutableObject<Throwable> error = new MutableObject<>();
        AtomicBoolean proceed = new AtomicBoolean(false);
        // Increase busy ref count and begin mutual exclusively lock process
        busyRefCounter.incrementRefCount();
        try {
            // BusyRefCounter is only increased when already busy or locked.
            // So when we are now locked, have increased ref count and ref == 1 it means we have mutual exclusively locked the holder.
            // If it's not when ref decreases to 1 it means we mutual exclusively lock the holder,
            // because ref won't be increased by scheduler when it's already busy.
            // combineSavingFuture / addSaveDependency used externally will continue to pause scheduling on a best-effort basis.
            // note: this is not so in later versions of C2ME, but we may still mutual exclusively lock it in per-holder executor
            synchronized (this) {
                ((BusyRefCounterBridge) busyRefCounter).arclight$addMutexListener(() -> {
                    // Further processing is limited to busy ticking due to already busy
                    // They may still try to cancel which will be acquired by our condition
                    proceed.setRelease(true);
                    ((BlockableEventLoopBridge_C2ME) blocker).arclight$wakeUpBlocker();
                });
            }
            // Do not just wait, do some tasks!
            blocker.managedBlock(() -> {
                Throwable err = condition.get();
                error.setValue(err);
                // If err != null, something has tried to cancel us. Got to move on now.
                return err != null || proceed.getAcquire();
            });
            // We have mutual exclusively locked the holder. Let's see what we should do.
            Throwable value = error.getValue();
            if (value == null) {
                runnable.run();
                emitter.onComplete();
            } else {
                ((BusyRefCounterBridge) busyRefCounter).arclight$removeMutexListener();
                emitter.onError(value);
            }
        } catch (Exception e) {
            ((BusyRefCounterBridge) busyRefCounter).arclight$removeMutexListener();
            emitter.onError(e);
        } finally {
            // Don't leave busy ref counter on even if something bad happens.
            busyRefCounter.decrementRefCount();
        }
    }

    @Override
    public void arclight$runBusyNow(CompletableEmitter emitter, Runnable runnable) {
        synchronized (this) {
            Assertions.assertTrue(isOpen(), "BUG: holder is already unloaded before unload event is sent");
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
