package io.izzel.arclight.common.mod.compat;

import io.izzel.arclight.common.mod.compat.c2me.C2MECompat;

public class CompatChecker {
    public static void check() {
        C2MECompat.preLoadForTransformation();
    }
}
