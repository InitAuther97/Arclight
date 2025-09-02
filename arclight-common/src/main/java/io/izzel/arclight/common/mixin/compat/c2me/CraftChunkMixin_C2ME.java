package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.core.world.level.entity.PersistentEntitySectionManagerBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.inject.InjectEntityBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.bukkit.World;
import org.bukkit.craftbukkit.v.CraftChunk;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(CraftChunk.class)
public abstract class CraftChunkMixin_C2ME {

    // @formatter:off
    @Shadow public abstract boolean isLoaded();
    @Shadow public abstract World getWorld();
    @Shadow public abstract CraftWorld getCraftWorld();
    @Shadow @Final private int x;
    @Shadow @Final private int z;
    // @formatter:on

    /**
     * @author InitAuther97
     * @reason C2ME submits the tasks to ServerChunkCache.MainThreadExecutor directly,
     * change to use managedBlock to provide better task throughput and C2ME compatibility.
     */
    @Overwrite(remap = false)
    public Entity[] getEntities() {
        if (!this.isLoaded()) {
            ArclightServer.LOGGER.warn("Chunk is not loaded", new Exception());
            this.getWorld().getChunkAt(this.x, this.z);
        }

        PersistentEntitySectionManager<net.minecraft.world.entity.Entity> entityManager = this.getCraftWorld().getHandle().entityManager;
        ServerChunkProviderBridge bridge = (ServerChunkProviderBridge) this.getCraftWorld().getHandle().getChunkSource();
        long pair = ChunkPos.asLong(this.x, this.z);
        if (!entityManager.areEntitiesLoaded(pair)) {
            bridge.arclight$managedBlockOnExecutor(() -> {
                if (!((PersistentEntitySectionManagerBridge) entityManager).isPending(pair)) {
                    entityManager.ensureChunkQueuedForLoad(pair);
                }

                entityManager.tick();
                return entityManager.areEntitiesLoaded(pair);
            });
        }

        return ((PersistentEntitySectionManagerBridge) entityManager).getEntities(new ChunkPos(this.x, this.z)).stream().map(InjectEntityBridge::bridge$getBukkitEntity).filter(Objects::nonNull).toArray(Entity[]::new);
    }
}
