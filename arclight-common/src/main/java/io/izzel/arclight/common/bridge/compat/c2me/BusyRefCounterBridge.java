package io.izzel.arclight.common.bridge.compat.c2me;

import io.reactivex.rxjava3.core.CompletableEmitter;
import net.minecraft.util.thread.BlockableEventLoop;

import java.util.function.Supplier;

public interface BusyRefCounterBridge {

    /**
     * Usage must form valid condition for mutex listener to work.
     * @see io.izzel.arclight.common.mixin.compat.c2me.ItemHolderMixin#arclight$runBusyNow(BlockableEventLoop, CompletableEmitter, Supplier, Runnable)
     * @param runnable called when mutual exclusively busy
     */
    void arclight$addMutexListener(Runnable runnable);
}
