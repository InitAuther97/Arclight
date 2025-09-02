package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.ABSENT)
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin_Chunk implements ChunkMapBridge {

    public final ArclightCallbackExecutor callbackExecutor = new ArclightCallbackExecutor();

    @Override
    public void arclight$addCallback(Runnable callback) {
        callbackExecutor.execute(callback);
    }

    @Override
    public void arclight$runCallbacks() {
        callbackExecutor.run();
    }
}
