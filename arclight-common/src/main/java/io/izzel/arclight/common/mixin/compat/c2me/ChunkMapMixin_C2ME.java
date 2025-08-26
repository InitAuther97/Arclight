package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin_C2ME implements ChunkMapBridge {
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void arclight$tickCallbacks(CallbackInfo ci) {
        arclight$runCallbacks();
    }
}
