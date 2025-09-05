package io.izzel.arclight.common.mixin.compat.c2me.debug;

import com.ishland.flowsched.scheduler.BusyRefCounter;
import io.izzel.arclight.common.bridge.compat.c2me.BusyRefCounterBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@LoadIfProperty("arclight.c2me.debug")
@Mixin(value = BusyRefCounter.class, remap = false)
public abstract class BusyRefCounterMixin_Debug implements BusyRefCounterBridge {

    @Unique
    private final Map<String, Exception> arclight$debug$busyReason = new HashMap<>();

    @Override
    public void arclight$debug$pushBusyReason(String reason) {
        arclight$debug$busyReason.put(reason, new Exception("Busy ref counter stack trace"));
    }

    @Override
    public void arclight$debug$popBusyReason(String reason) {
        arclight$debug$busyReason.remove(reason);
    }

    @Override
    public Map<String, Exception> arclight$debug$allBusyReason() {
        return Map.copyOf(arclight$debug$busyReason);
    }
}
