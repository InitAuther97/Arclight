package io.izzel.arclight.common.mixin.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemStatus;
import io.izzel.arclight.common.bridge.compat.c2me.ServerAccessibleBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Pseudo
@Mixin(targets = "com.ishland.flowsched.scheduler.StatusAdvancingScheduler")
public class StatusAdvancingSchedulerMixin {

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/ishland/flowsched/scheduler/ItemHolder;isBusy()Z")), at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/ishland/flowsched/scheduler/ItemHolder;consolidateMarkDirty(Lcom/ishland/flowsched/scheduler/StatusAdvancingScheduler;)V"))
    private void arclight$beforeBusyMarkDirty(
            CallbackInfoReturnable<Boolean> cir,
            boolean hasWork,
            Object key,
            ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder,
            ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> var4,
            ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> current,
            ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> nextStatus,
            ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> upgradingStatusTo,
            ItemStatus<ChunkPos, ChunkState, ChunkLoadingContext> projectedCurrent
    ) {
        if (current == NewChunkStatus.SERVER_ACCESSIBLE) {
            ((ServerAccessibleBridge) current).arclight$updateStatusBusy(holder, nextStatus);
        }
    }
}
