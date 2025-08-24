package io.izzel.arclight.common.mod.server.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

public class IllegalChunkAccessException extends RuntimeException {
    public IllegalChunkAccessException(ResourceLocation worldIn, ChunkPos eventIn, ChunkPos illegalAccess) {
        super(String.format("In world %s, during chunk event at %s, attempting to load chunk %s", worldIn, eventIn, illegalAccess));
    }
}
