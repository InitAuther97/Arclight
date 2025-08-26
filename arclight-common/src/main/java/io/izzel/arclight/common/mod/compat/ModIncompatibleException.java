package io.izzel.arclight.common.mod.compat;

public class ModIncompatibleException extends RuntimeException {
    public ModIncompatibleException(String message) {
        super(message);
    }
    public ModIncompatibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
