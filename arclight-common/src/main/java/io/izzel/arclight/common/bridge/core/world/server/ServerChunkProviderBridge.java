package io.izzel.arclight.common.bridge.core.world.server;

import java.io.IOException;
import java.util.function.BooleanSupplier;

import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.LevelChunk;

public interface ServerChunkProviderBridge {

    void bridge$close(boolean save) throws IOException;

    void bridge$purgeUnload();

    boolean bridge$tickDistanceManager();

    boolean bridge$isChunkLoaded(int x, int z);

    ThreadedLevelLightEngine bridge$getLightManager();

    default void arclight$setChunkEvent(long pos, LevelChunk unloading) {
        // no-op
    }

    void arclight$setMainThreadExecutor(BlockableEventLoop<Runnable> executor);

    void arclight$managedBlockOnExecutor(BooleanSupplier until);

    boolean arclight$isMainThread();
}