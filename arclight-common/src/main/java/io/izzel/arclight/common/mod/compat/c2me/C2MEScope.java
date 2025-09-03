package io.izzel.arclight.common.mod.compat.c2me;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.internal.schedulers.ExecutorScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public class C2MEScope {

    public static final Throwable PENDING = new Throwable("Pending") {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    };

    public static final CancellationException CANCELLED = new CancellationException("Cancelled") {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    };

    public static final BooleanSupplier TRUE = () -> true;

    /**
     * Should not be used to schedule periodic tasks!
     */
    public static final Scheduler SCHEDULER_BACKED_BY_SERVER;

    public static final BlockableEventLoop<Runnable> MAIN_THREAD_MAILBOX;

    static {
        MinecraftServer server = ArclightServer.getMinecraftServer();
        if (server == null) {
            ArclightServer.LOGGER.fatal("C2MEScope is accessed way too early before MinecraftServer is initialized");
            throw new IllegalStateException("C2MEScope is accessed before MinecraftServer is initialized");
        }
        MAIN_THREAD_MAILBOX = new BlockableEventLoop<>("Arclight C2ME Chunk Event") {
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
        SCHEDULER_BACKED_BY_SERVER = new ExecutorScheduler(MAIN_THREAD_MAILBOX, false, false);
    }
}
