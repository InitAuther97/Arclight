package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.compat.c2me.BlockableEventLoopBridge_C2ME;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.locks.LockSupport;

@Mixin(BlockableEventLoop.class)
public abstract class BlockableEventLoopMixin_C2ME implements BlockableEventLoopBridge_C2ME {

    @Shadow protected abstract Thread getRunningThread();

    @Shadow private int blockingCount;

    @Override
    public void arclight$wakeUpBlocker() {
        if (blockingCount == 0) return;
        // There's a chance that the blocker is active when we complete, and he went away already when we just tried to wake him up
        // He may also wake up before unpark() and went away. Best-effort basis.
        LockSupport.unpark(getRunningThread());
    }
}
