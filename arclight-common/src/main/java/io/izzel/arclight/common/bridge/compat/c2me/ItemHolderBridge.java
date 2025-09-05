package io.izzel.arclight.common.bridge.compat.c2me;

import io.reactivex.rxjava3.core.CompletableEmitter;
import net.minecraft.util.thread.BlockableEventLoop;

import java.util.function.Supplier;

public interface ItemHolderBridge {
    void arclight$runBusyNow(BlockableEventLoop<?> blocker, CompletableEmitter emitter, Supplier<Throwable> condition, Runnable runnable);
    void arclight$runBusyNow(CompletableEmitter emitter, Runnable runnable);
    void arclight$debug$pushNextBusyReason(String reason);
}
