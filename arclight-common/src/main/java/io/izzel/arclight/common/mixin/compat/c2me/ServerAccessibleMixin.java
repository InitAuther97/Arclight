package io.izzel.arclight.common.mixin.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.flowsched.scheduler.Cancellable;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemStatus;
import io.izzel.arclight.common.bridge.compat.c2me.*;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(targets = "com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessible", remap = false)
public abstract class ServerAccessibleMixin extends NewChunkStatus implements ServerAccessibleBridge {

    ServerAccessibleMixin() {
        super(-1, null);
    }

    public void arclight$updateStatusBusy(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder, ItemStatus<?, ChunkState, ChunkLoadingContext> going) {
        NewChunkHolderVanillaInterfaceBridge chunkHolder = (NewChunkHolderVanillaInterfaceBridge) holder.getUserData().get();
        chunkHolder.arclight$cancelIfNecessary(ordinal() < going.ordinal());
    }

    @Inject(method = "upgradeToThis*", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;runAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private void arclight$scheduleLoad(ChunkLoadingContext ctx, Cancellable cancellable, CallbackInfoReturnable<CompletableFuture<?>> cir) {
        ItemHolder<?, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder = ctx.holder();
        NewChunkHolderVanillaInterfaceBridge chunkHolder = (NewChunkHolderVanillaInterfaceBridge) holder.getUserData().get();
        chunkHolder.arclight$scheduleLoad(ctx);
    }

    @Redirect(method = "downgradeFromThis*", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;runAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<?> arclight$scheduleUnload(Runnable action, Executor executor, ChunkLoadingContext ctx, Cancellable cancellable) {
        ItemHolder<?, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder = ctx.holder();
        NewChunkHolderVanillaInterfaceBridge chunkHolder = (NewChunkHolderVanillaInterfaceBridge) holder.getUserData().get();
        return chunkHolder.arclight$scheduleUnload(ctx, cancellable).thenRunAsync(action, executor);
    }
}