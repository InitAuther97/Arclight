package io.izzel.arclight.common.mixin.core.world.level.entity;

import io.izzel.arclight.common.bridge.core.world.level.entity.PersistentEntitySectionManagerBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.ABSENT)
@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin_Chunk<T> implements PersistentEntitySectionManagerBridge {

    @Shadow @Final public EntityPersistentStorage<T> permanentStorage;

    @Unique
    private boolean arclight$fireEvent = false;

    @Decorate(method = "storeChunkSections", inject = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityPersistentStorage;storeEntities(Lnet/minecraft/world/level/entity/ChunkEntities;)V"))
    private void arclight$fireUnload(long pos, @Local(ordinal = -1) List<T> list) {
        if (arclight$fireEvent) {
            CraftEventFactory.callEntitiesUnloadEvent(((EntityStorage) permanentStorage).level, new ChunkPos(pos),
                    list.stream().map(entity -> (Entity) entity).collect(Collectors.toList()));
        }
    }

    @Inject(method = "storeChunkSections", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityPersistentStorage;storeEntities(Lnet/minecraft/world/level/entity/ChunkEntities;)V"))
    private void arclight$resetFlag(long pos, Consumer<T> consumer, CallbackInfoReturnable<Boolean> cir) {
        arclight$fireEvent = false;
    }

    @Inject(method = "processChunkUnload", at = @At("HEAD"))
    private void arclight$fireEvent(long pChunkPosValue, CallbackInfoReturnable<Boolean> cir) {
        arclight$fireEvent = true;
    }

    @Inject(method = "processPendingLoads", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, remap = false, target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
    private void arclight$fireLoad(CallbackInfo ci, ChunkEntities<T> chunkEntities) {
        List<Entity> entities = getEntities(chunkEntities.getPos());
        CraftEventFactory.callEntitiesLoadEvent(((EntityStorage) permanentStorage).level, chunkEntities.getPos(), entities);
    }
}
