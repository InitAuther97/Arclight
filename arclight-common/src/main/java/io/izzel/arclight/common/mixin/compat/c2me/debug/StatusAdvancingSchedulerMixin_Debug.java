package io.izzel.arclight.common.mixin.compat.c2me.debug;

import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.StatusAdvancingScheduler;
import io.izzel.arclight.common.bridge.compat.c2me.ItemHolderBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfProperty;
import io.reactivex.rxjava3.core.Completable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletionStage;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@LoadIfProperty("arclight.c2me.debug")
@Mixin(value = StatusAdvancingScheduler.class, remap = false)
public class StatusAdvancingSchedulerMixin_Debug {

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/ishland/flowsched/scheduler/ItemHolder;submitOp(Ljava/util/concurrent/CompletionStage;)V"))
    private void arclight$debug$pushBusyReasonTick0(ItemHolder instance, CompletionStage<Void> op) {
        ((ItemHolderBridge)instance).arclight$debug$pushNextBusyReason("Cleanup dependencies unloaded");
        instance.submitOp(op);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lcom/ishland/flowsched/scheduler/ItemHolder;submitOp(Ljava/util/concurrent/CompletionStage;)V"))
    private void arclight$debug$pushBusyReasonTick1(ItemHolder instance, CompletionStage<Void> op) {
        ((ItemHolderBridge)instance).arclight$debug$pushNextBusyReason("Cleanup dependencies idle");
        instance.submitOp(op);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 2, target = "Lcom/ishland/flowsched/scheduler/ItemHolder;submitOp(Ljava/util/concurrent/CompletionStage;)V"))
    private void arclight$debug$pushBusyReasonTick2(ItemHolder instance, CompletionStage<Void> op) {
        ((ItemHolderBridge)instance).arclight$debug$pushNextBusyReason("Tick schedule advanceStatus0");
        instance.submitOp(op);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 3, target = "Lcom/ishland/flowsched/scheduler/ItemHolder;submitOp(Ljava/util/concurrent/CompletionStage;)V"))
    private void arclight$debug$pushBusyReasonTick3(ItemHolder instance, CompletionStage<Void> op) {
        ((ItemHolderBridge)instance).arclight$debug$pushNextBusyReason("Tick schedule downgradeStatus0");
        instance.submitOp(op);
    }

    @Redirect(method = "advanceStatus0", at = @At(value = "INVOKE", target = "Lcom/ishland/flowsched/scheduler/ItemHolder;subscribeOp(Lio/reactivex/rxjava3/core/Completable;)V"))
    private void arclight$debug$pushBusyReasonAdvanceStatus0(ItemHolder instance, Completable op) {
        ((ItemHolderBridge)instance).arclight$debug$pushNextBusyReason("advanceStatus0 advancing");
        instance.subscribeOp(op);
    }

    @Redirect(method = "downgradeStatus0", at = @At(value = "INVOKE", target = "Lcom/ishland/flowsched/scheduler/ItemHolder;subscribeOp(Lio/reactivex/rxjava3/core/Completable;)V"))
    private void arclight$debug$pushBusyReasonDowngradeStatus0(ItemHolder instance, Completable op) {
        ((ItemHolderBridge)instance).arclight$debug$pushNextBusyReason("downgradeStatus0 downgrading");
        instance.subscribeOp(op);
    }
}
