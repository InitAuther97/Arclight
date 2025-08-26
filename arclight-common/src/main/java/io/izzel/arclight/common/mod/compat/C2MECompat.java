package io.izzel.arclight.common.mod.compat;

import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.throwables.MixinError;

public class C2MECompat {
    public static void preLoadForTransformation() {
        if (ArclightCommon.api().isModLoaded("c2me")) {
            try {
                Class.forName("com.ishland.c2me.rewrites.chunksystem.common.statuses.ServerAccessible");
            } catch (ClassNotFoundException e) {
                ArclightServer.LOGGER.fatal("Failed to pre-load C2ME classes for transformation! Is it up to date?", e);
                throw new ModIncompatibleException("Failed to pre-load C2ME classes for transformation! Is it up to date?", e);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof MixinError me) {
                    ArclightServer.LOGGER.fatal("Transformation for C2ME classes failed, is it up to date?", e);
                    throw new ModIncompatibleException("Transformation for C2ME classes failed, is it up to date?", me);
                } else {
                    ArclightServer.LOGGER.fatal("Unexpected failure when trying to pre-load C2ME classes for transformation.", e);
                    throw e;
                }
            }
        }
    }

    public static void callChunkLoad(LevelChunk chunk) {
        ((ChunkBridge) chunk).bridge$loadCallback();
    }

    public static void callChunkUnload(LevelChunk chunk) {
        ((ChunkBridge) chunk).bridge$unloadCallback();
    }
}
