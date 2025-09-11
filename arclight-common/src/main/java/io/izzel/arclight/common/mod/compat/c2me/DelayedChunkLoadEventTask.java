package io.izzel.arclight.common.mod.compat.c2me;

import com.ishland.c2me.rewrites.chunksystem.common.NewChunkStatus;
import com.ishland.flowsched.util.Assertions;
import io.izzel.arclight.common.bridge.compat.c2me.BlockableEventLoopBridge_C2ME;
import io.izzel.arclight.common.bridge.compat.c2me.ItemHolderBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;
import net.minecraft.util.thread.BlockableEventLoop;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class DelayedChunkLoadEventTask extends ChunkLoadEventTask {

    protected BlockableEventLoop<ChunkEventTask> initiator;
    protected CompletableEmitter loadEvent;

    public DelayedChunkLoadEventTask(ChunkLoadEventTask task) {
        super(task);
    }

    @Override
    public void bind(BlockableEventLoop<ChunkEventTask> task) {
        super.bind(task);
    }

    @Override
    protected void initTask() {
        taskFuture = Completable.create(emitter -> this.loadEvent = emitter).cache();
    }

    @Override
    public void run() {
        Assertions.assertTrue(initiator.isSameThread(), "BUG: trying to managedBlock off the running thread of the chunk event mailbox");
        // Increase busy ref count and begin mutual exclusively lock process, this will set the emitter for us
        holder.subscribeOp(Completable.create(this::runActual).doOnError(th -> {
            if (th == C2MEScope.MUTEX_TIMEOUT) {
                initiator.tell(this);
            }
        }));
    }

    private void runActual(CompletableEmitter emitter) {
        MutableObject<Throwable> error = new MutableObject<>();
        AtomicBoolean proceed = new AtomicBoolean(false);
        // BusyRefCounter is only increased when already busy or locked.
        // So when we are now locked, have increased ref count and ref == 1 it means we have mutual exclusively locked the holder.
        // If it's not when ref decreases to 1 it means we mutual exclusively lock the holder,
        // because ref won't be increased by scheduler when it's already busy.
        // combineSavingFuture / addSaveDependency used externally will continue to pause scheduling on a best-effort basis.
        // note: this is not so in later versions of C2ME, but we may still mutual exclusively lock it in per-holder executor
        synchronized (holder) {
            final var future = holder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE);
            if (!future.isDone() || future.isCompletedExceptionally()) {
                emitter.onError(C2MEScope.LOAD_EVENT_CANCELLED);
            }
            ((ItemHolderBridge) holder).arclight$addMutexListener(() -> {
                // Further processing is limited to busy ticking due to already busy
                // They may still try to cancel which will be acquired by our condition
                proceed.setRelease(true);
                ((BlockableEventLoopBridge_C2ME) server).arclight$wakeUpBlocker();
            });
        }
        // Do not just wait, do some tasks!
        server.managedBlock(() -> {
            if (!((MinecraftServerBridge)server).arclight$haveTime()) {
                error.setValue(C2MEScope.MUTEX_TIMEOUT);
                return true;
            }
            final var future = holder.getFutureForStatus(NewChunkStatus.SERVER_ACCESSIBLE);
            if (!future.isDone() || future.isCompletedExceptionally()) {
                emitter.onError(C2MEScope.LOAD_EVENT_CANCELLED);
                error.setValue(C2MEScope.LOAD_EVENT_CANCELLED);
                return true;
            }
            // If err != null, something has tried to cancel us. Got to move on now.
            return proceed.getAcquire();
        });
        // We have mutual exclusively locked the holder. Let's see what we should do.
        Throwable value = error.getValue();
        if (value == null) {
            ((ChunkBridge) holder.getItem().get().chunk()).bridge$loadCallback();
            emitter.onComplete();
        } else {
            ((ItemHolderBridge) holder).arclight$removeMutexListener();
        }
    }
}
