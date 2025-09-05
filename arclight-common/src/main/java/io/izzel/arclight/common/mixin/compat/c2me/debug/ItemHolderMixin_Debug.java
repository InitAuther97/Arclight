package io.izzel.arclight.common.mixin.compat.c2me.debug;

import com.ishland.flowsched.scheduler.BusyRefCounter;
import com.ishland.flowsched.scheduler.ItemHolder;
import io.izzel.arclight.common.bridge.compat.c2me.BusyRefCounterBridge;
import io.izzel.arclight.common.bridge.compat.c2me.ItemHolderBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfProperty;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@LoadIfProperty("arclight.c2me.debug")
@Mixin(value = ItemHolder.class, remap = false)
public abstract class ItemHolderMixin_Debug implements ItemHolderBridge {

    @Shadow @Final private BusyRefCounter busyRefCounter;

    @Unique
    private String arclight$busyReason;

    @Override
    public void arclight$debug$pushNextBusyReason(String reason) {
        arclight$busyReason = reason;
    }

    @Redirect(method = "submitOp", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletionStage;whenComplete(Ljava/util/function/BiConsumer;)Ljava/util/concurrent/CompletionStage;"))
    private CompletionStage<Void> arclight$pushBusyReason(CompletionStage<Void> instance, BiConsumer<? super Void, ? super Throwable> biConsumer) {
        String busyReason = arclight$busyReason;
        arclight$busyReason = null;
        ((BusyRefCounterBridge) busyRefCounter).arclight$debug$pushBusyReason(busyReason);
        return instance.whenComplete(biConsumer)
                .whenComplete((none, th) -> ((BusyRefCounterBridge) busyRefCounter).arclight$debug$popBusyReason(busyReason));
    }

    @Redirect(method = "subscribeOp", at = @At(value = "INVOKE", target = "Lio/reactivex/rxjava3/core/Completable;subscribe(Lio/reactivex/rxjava3/functions/Action;)Lio/reactivex/rxjava3/disposables/Disposable;"))
    private Disposable arclight$subscribeBusyReason(Completable instance, Action action) {
        String busyReason = arclight$busyReason;
        arclight$busyReason = null;
        ((BusyRefCounterBridge) busyRefCounter).arclight$debug$pushBusyReason(busyReason);
        return instance.subscribe(() -> {
            action.run();
            ((BusyRefCounterBridge) busyRefCounter).arclight$debug$popBusyReason(busyReason);
        });
    }
}
