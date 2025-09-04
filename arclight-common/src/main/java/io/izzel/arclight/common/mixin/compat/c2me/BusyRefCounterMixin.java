package io.izzel.arclight.common.mixin.compat.c2me;

import com.ishland.flowsched.scheduler.BusyRefCounter;
import com.ishland.flowsched.util.Assertions;
import io.izzel.arclight.common.bridge.compat.c2me.BusyRefCounterBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import org.spongepowered.asm.mixin.*;

@Mixin(value = BusyRefCounter.class, remap = false)
public abstract class BusyRefCounterMixin implements BusyRefCounterBridge {

    @Shadow private int counter;
    @Shadow @Final private ReferenceList<Runnable> onComplete;
    @Shadow private Runnable onCompleteOnce;

    @Unique
    private Runnable arclight$mutexListener;

    @Override
    public synchronized void arclight$addMutexListener(Runnable runnable) {
        Assertions.assertTrue(counter > 0, "BUG: ref count is not increased before adding mutual exclusive listener");
        Assertions.assertTrue(arclight$mutexListener == null, "BUG: multiple mutual exclusive listeners");
        if (counter == 1) {
            // it's already mutually locked.
            runnable.run();
        } else {
            arclight$mutexListener = runnable;
        }
    }

    /**
     * @author InitAuther97
     * @reason add mutex listener
     */
    @Overwrite
    public void decrementRefCount() {
        Runnable[] onCompleteArray = null;
        Runnable onCompleteOnce = null;
        Runnable mutexListener = null;
        synchronized(this) {
            Assertions.assertTrue(this.counter > 0);
            if (--this.counter == 0) {
                Assertions.assertTrue(arclight$mutexListener == null, "BUG: mutex listener is released before it's acquired and called");
                onCompleteArray = this.onComplete.toArray(Runnable[]::new);
                this.onComplete.clear();
            } else if (this.counter == 1) {
                mutexListener = arclight$mutexListener;
                arclight$mutexListener = null;
            }

            onCompleteOnce = this.onCompleteOnce;
            this.onCompleteOnce = null;
        }

        if (mutexListener != null) {
            try {
                mutexListener.run();
            } catch (Exception e) {
                ArclightServer.LOGGER.error("Mutex listener failed with an unexpected exception", e);
            }
        }

        if (onCompleteArray != null) {
            for(Runnable runnable : onCompleteArray) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        if (onCompleteOnce != null) {
            try {
                onCompleteOnce.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }
}
