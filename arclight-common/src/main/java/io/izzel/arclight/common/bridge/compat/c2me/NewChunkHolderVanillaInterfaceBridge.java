package io.izzel.arclight.common.bridge.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.flowsched.scheduler.Cancellable;

import java.util.concurrent.CompletableFuture;

public interface NewChunkHolderVanillaInterfaceBridge {
    void arclight$scheduleLoad(ChunkLoadingContext ctx);
    CompletableFuture<Void> arclight$scheduleUnload(ChunkLoadingContext ctx, Cancellable cancellable);
    void arclight$cancelIfNecessary(boolean isUpgrade);
}
