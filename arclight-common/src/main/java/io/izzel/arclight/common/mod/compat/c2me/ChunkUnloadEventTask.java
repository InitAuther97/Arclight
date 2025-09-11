package io.izzel.arclight.common.mod.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.util.Assertions;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.reactivex.rxjava3.core.Completable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkUnloadEventTask implements ChunkEventTask {

    private final ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder;
    private final LevelChunk chunkIn;

    public ChunkUnloadEventTask(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder, LevelChunk chunkIn) {
        this.holder = holder;
        this.chunkIn = chunkIn;
    }

    public ChunkUnloadEventTask delayed() {
        throw new IllegalStateException("BUG: ChunkUnloadEventTask should not be delayed");
    }

    @Override
    public Completable getTaskFuture() {
        return null;
    }

    @Override
    public void run() {
        Assertions.assertTrue(holder.isOpen(), "BUG: holder is already unloaded before unload event is sent");
        holder.subscribeOp(Completable.create(emitter -> ((ChunkBridge)chunkIn).bridge$loadCallback()));
    }
}
