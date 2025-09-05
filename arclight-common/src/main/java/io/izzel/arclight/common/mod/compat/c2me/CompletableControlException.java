package io.izzel.arclight.common.mod.compat.c2me;

import java.util.concurrent.CancellationException;

public class CompletableControlException extends CancellationException {
    public CompletableControlException(String str) {
        super(str);
    }

    /*@Override
    public Throwable fillInStackTrace() {
        return this; // no-op
    }*/
}
