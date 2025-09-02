package io.izzel.arclight.common.bridge.compat.c2me;

import io.reactivex.rxjava3.core.CompletableEmitter;

public interface ItemHolderBridge {
    void arclight$runBusyNow(CompletableEmitter emitter, Runnable runnable);
}
