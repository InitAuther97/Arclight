package io.izzel.arclight.common.mod.util;

public class FastClassNotFoundException extends ClassNotFoundException {

    public FastClassNotFoundException(String name) {
        super(name);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
