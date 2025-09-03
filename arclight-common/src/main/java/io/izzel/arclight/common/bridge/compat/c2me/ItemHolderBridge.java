package io.izzel.arclight.common.bridge.compat.c2me;

import io.reactivex.rxjava3.core.CompletableEmitter;

import java.util.function.BooleanSupplier;

public interface ItemHolderBridge {
    void arclight$runBusyNow(CompletableEmitter emitter, BooleanSupplier condition, Runnable runnable);
}
