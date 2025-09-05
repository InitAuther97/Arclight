package io.izzel.arclight.common.mod.compat.c2me;

import com.ishland.flowsched.util.Assertions;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

public class C2MEScope {

    public static final CancellationException LOAD_EVENT_CANCELLED = new CompletableControlException("Load event cancelled");
    public static final CancellationException UNLOAD_CANCELLED = new CompletableControlException("Unload cancelled");
    public static final CancellationException MUTEX_TIMEOUT = new CompletableControlException("Main thread mutex timeout");

    public static final BooleanSupplier TRUE = () -> true;

    // Should not be used to schedule periodic tasks!
    public static final Scheduler SCHEDULER_BACKED_BY_SERVER_TELL;
    public static final Scheduler SCHEDULER_BACKED_BY_SERVER;
    public static final Scheduler SCHEDULER_BACKED_BY_SERVER_REENTRANT;

    // We cannot submit directly to MinecraftServer because blocking on it may pollTask from ServerChunkCache.
    // Given that a chunk event can managedBlock on MainThreadExecutor to get its entities which may trigger
    // another chunk load, submitting to MinecraftServer may cause it to run in-place because even though
    // MinecraftServer is reentrant, in-place execution won't be considered as running a task.
    // Thus, we use an independent mailbox and poll it when MinecraftServer is polling tasks.
    //
    // It's impossible to managedBlock on MinecraftServer when the mailbox is running since it will also poll
    // this mailbox. But by tampering with pollTask() the mailbox is not polled when reentrant.
    //
    // Use the reentrant implementation because we don't want to run chunk load task in a ChunkEvent.
    // This may happen when MinecraftServer pollTask from MAIN_THREAD_MAILBOX triggering a ChunkEvent listener.
    // Then the listener may use getEntities which may...
    //
    // 1) managedBlock on ServerChunkCache.MainThreadExecutor
    //    This may issue the next ChunkLoadEvent/ChunkUnloadEvent on this mailbox.
    //    If the fullChunkFuture is already completed, since we're on the main thread at this very moment,
    //    the event may run in-place and occur during the first ChunkEvent. (very bad!)
    //
    // 2) Tick PersistentEntitySectionManager which may fire EntitiesLoadEvent for an already unloaded chunk. (BUG)
    //    Attempt to use getChunk().getEntities() will issue chunk load for this chunk. (dangerous!!!)
    //
    // By tampering with reentrant implementation we solve 1) because the Runnable is still queued during task execution.
    public static final ReentrantBlockableEventLoop<Runnable> MAIN_THREAD_MAILBOX;

    static {
        MinecraftServer server = ArclightServer.getMinecraftServer();
        if (server == null) {
            ArclightServer.LOGGER.fatal("C2MEScope is accessed way too early before MinecraftServer is initialized");
            throw new IllegalStateException("C2MEScope is accessed before MinecraftServer is initialized");
        }
        MAIN_THREAD_MAILBOX = new ReentrantBlockableEventLoop<>("Arclight C2ME ChunkEvent Mailbox") {

            @Override
            public void executeIfPossible(Runnable runnable) {
                if (!isSameThread()) { // Skip reentrant check
                    this.tell(runnable);
                } else {
                    doRunTask(runnable);
                }
            }

            @Override
            public void execute(Runnable runnable) {
                if (this.scheduleExecutables()) {
                    this.tell(runnable);
                } else {
                    doRunTask(runnable);
                }
            }

            @Override
            public CompletableFuture<Void> submit(Runnable runnable) {
                if (this.scheduleExecutables()) {
                    return CompletableFuture.runAsync(runnable, this);
                } else {
                    doRunTask(runnable);
                    return CompletableFuture.completedFuture(null);
                }
            }

            @Override
            public boolean pollTask() {
                return !runningTask() && super.pollTask();
            }

            @Override
            public void managedBlock(BooleanSupplier booleanSupplier) {
                Assertions.assertTrue(!runningTask(), "BUG: reentrant managedBlock on chunk event mailbox");
                Assertions.assertTrue(isSameThread(), "BUG: managedBlock on chunk event mailbox off thread");
                super.managedBlock(booleanSupplier);
            }

            @Override
            protected Runnable wrapRunnable(Runnable runnable) {
                return runnable;
            }

            @Override
            protected boolean shouldRun(Runnable runnable) {
                return true;
            }

            @Override
            protected Thread getRunningThread() {
                return server.getRunningThread();
            }
        };
        SCHEDULER_BACKED_BY_SERVER_TELL = Schedulers.from(MAIN_THREAD_MAILBOX::tell);
        SCHEDULER_BACKED_BY_SERVER = Schedulers.from(MAIN_THREAD_MAILBOX::executeIfPossible);
        SCHEDULER_BACKED_BY_SERVER_REENTRANT = Schedulers.from(MAIN_THREAD_MAILBOX);
    }
}
