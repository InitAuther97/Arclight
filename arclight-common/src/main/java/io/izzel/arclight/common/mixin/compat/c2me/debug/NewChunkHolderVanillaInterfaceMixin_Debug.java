package io.izzel.arclight.common.mixin.compat.c2me.debug;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.flowsched.scheduler.ItemHolder;
import io.izzel.arclight.common.bridge.compat.c2me.ItemHolderBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfProperty;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@LoadIfProperty("arclight.c2me.debug")
@Mixin(NewChunkHolderVanillaInterface.class)
public abstract class NewChunkHolderVanillaInterfaceMixin_Debug extends ChunkHolder {
    @Shadow @Final private ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> newHolder;

    protected NewChunkHolderVanillaInterfaceMixin_Debug(ChunkPos chunkPos, int i, LevelHeightAccessor levelHeightAccessor, LevelLightEngine levelLightEngine, LevelChangeListener levelChangeListener, PlayerProvider playerProvider) {
        super(chunkPos, i, levelHeightAccessor, levelLightEngine, levelChangeListener, playerProvider);
    }

    @Inject(method = "addSaveDependency", at = @At(value = "INVOKE", target = "Lcom/ishland/flowsched/scheduler/ItemHolder;submitOp(Ljava/util/concurrent/CompletionStage;)V"))
    private void arclight$debug$pushBusyReasonAddSaveDependencyExternal(CompletableFuture<?> savingFuture, CallbackInfo ci) {
        ArclightCaptures.externallySaveDependencyStackTrace(getPos(), Exception::new);
        ((ItemHolderBridge) newHolder).arclight$debug$pushNextBusyReason("externally combineSavingFuture");
        savingFuture.whenComplete((none, th) -> ArclightCaptures.externallySaveDependencyStackTrace(getPos(), null));
    }
}
