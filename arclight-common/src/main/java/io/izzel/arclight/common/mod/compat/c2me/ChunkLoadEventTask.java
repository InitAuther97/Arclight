package io.izzel.arclight.common.mod.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkState;
import com.ishland.c2me.rewrites.chunksystem.common.NewChunkHolderVanillaInterface;
import com.ishland.flowsched.scheduler.ItemHolder;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.reactivex.rxjava3.core.Completable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;

public class ChunkLoadEventTask implements ChunkEventTask{

    protected final ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder;
    protected final MinecraftServer server;
    protected Completable taskFuture;

    protected void initTask() {
        taskFuture = Completable.create(emitter -> ((ChunkBridge) holder.getItem().get().chunk()).bridge$loadCallback()).cache();
    }

    public ChunkLoadEventTask(ItemHolder<ChunkPos, ChunkState, ChunkLoadingContext, NewChunkHolderVanillaInterface> holder, MinecraftServer server) {
        this.holder = holder;
        this.server = server;
        initTask();
    }

    public ChunkLoadEventTask(ChunkLoadEventTask task) {
        this.holder = task.holder;
        this.server = task.server;
        initTask();
    }

    @Override
    public ChunkEventTask delayed() {
        return new DelayedChunkLoadEventTask(this);
    }

    @Override
    public Completable getTaskFuture() {
        return taskFuture;
    }

    @Override
    public void run() {
        holder.subscribeOp(taskFuture);
    }
}
