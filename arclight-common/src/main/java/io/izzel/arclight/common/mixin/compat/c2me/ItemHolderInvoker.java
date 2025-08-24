package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(targets = "com.ishland.flowsched.scheduler.ItemHolder")
public interface ItemHolderInvoker<K, V, Ctx, UserData> {

    @Coerce
    @Invoker("getFutureForStatus")
    CompletableFuture<Void> arclight$getFutureForStatusSafely(NewChunkStatusAccessor status);

    @Invoker("getItem")
    AtomicReference<V> arclight$getItem();
}
