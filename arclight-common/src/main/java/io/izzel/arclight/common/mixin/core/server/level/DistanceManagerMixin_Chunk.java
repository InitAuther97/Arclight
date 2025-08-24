package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.world.server.TicketManagerBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedList;
import java.util.Queue;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.ABSENT)
@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin_Chunk implements TicketManagerBridge {

    @Unique
    private Queue<ChunkHolder> arclight$scheduleUpdatingQueue = new LinkedList<>();

    @Override
    public void arclight$offerUpdate(ChunkHolder holder) {
        arclight$scheduleUpdatingQueue.add(holder);
    }

    @Decorate(method = "runAllUpdates", inject = true, at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"))
    private void arclight$runQueuedUpdates(ChunkMap map) {
        final var queue = arclight$scheduleUpdatingQueue;
        for (ChunkHolder now = queue.poll(); now != null; now = queue.poll()) {
            ((ChunkHolderBridge) now).bridge$callEventIfUnloading(map);
        }
    }
}
