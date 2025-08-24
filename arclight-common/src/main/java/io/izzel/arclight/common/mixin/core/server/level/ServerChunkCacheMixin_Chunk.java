package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.ABSENT)
@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin_Chunk {

    // InitAuther97: (w/o C2ME) need to skip loading the chunk if it's now scheduled to unload.
    @Decorate(method = "getChunkFutureMainThread", require = 0, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerChunkCache;getVisibleChunkIfPresent(J)Lnet/minecraft/server/level/ChunkHolder;"))
    private ChunkHolder arclight$skipLoadIfUnloading(ServerChunkCache instance, long l, @Local(ordinal = -1) boolean flag) throws Throwable {
        ChunkHolder holder = (ChunkHolder) DecorationOps.callsite().invoke(instance, l);
        if (holder != null) {
            flag = flag && !((ChunkHolderBridge) holder).arclight$isCurrentlyUnloading();
        }
        DecorationOps.blackhole().invoke(flag);
        return holder;
    }

    // InitAuther97: (w/o C2ME) the chunk is still loaded when an event is called in updateFutures, where the ticketLevel is the new state and the oldTicketLevel is the old state
    @Redirect(method = "chunkAbsent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;getTicketLevel()I"))
    public int arclight$useOldTicketLevel(ChunkHolder chunkHolder) {
        return ((ChunkHolderBridge) chunkHolder).bridge$getOldTicketLevel();
    }
}
