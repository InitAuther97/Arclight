package io.izzel.arclight.common.bridge.core.world.level.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

public interface PersistentEntitySectionManagerBridge {
    List<Entity> getEntities(ChunkPos posIn);
    boolean isPending(long coordIn);
}
