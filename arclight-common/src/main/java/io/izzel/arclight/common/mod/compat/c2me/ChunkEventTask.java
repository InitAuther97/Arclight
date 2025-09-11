package io.izzel.arclight.common.mod.compat.c2me;

import io.reactivex.rxjava3.core.Completable;
import net.minecraft.util.thread.BlockableEventLoop;

public interface ChunkEventTask extends Runnable {

    ChunkEventTask delayed();
    Completable getTaskFuture();
    default void bind(BlockableEventLoop<ChunkEventTask> task) {
    }
}
