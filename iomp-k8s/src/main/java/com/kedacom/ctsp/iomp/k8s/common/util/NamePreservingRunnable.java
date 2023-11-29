package com.kedacom.ctsp.iomp.k8s.common.util;


import lombok.extern.slf4j.Slf4j;

/**
 * @author keda
 */
@Slf4j
public class NamePreservingRunnable implements Runnable {
    /** The runnable name */
    private final String newName;

    /** The runnable task */
    private final Runnable runnable;

    /**
     * Creates a new instance of NamePreservingRunnable.
     *
     * @param runnable The underlying runnable
     * @param newName The runnable's name
     */
    public NamePreservingRunnable(Runnable runnable, String newName) {
        this.runnable = runnable;
        this.newName = newName;
    }

    /**
     * Run the runnable after having renamed the current thread's name
     * to the new name. When the runnable has completed, set back the
     * current thread name back to its origin.
     */
    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        String oldName = currentThread.getName();

        if (newName != null) {
            setName(currentThread, newName);
        }

        try {
            runnable.run();
        } finally {
            setName(currentThread, oldName);
        }
    }

    /**
     * Wraps {@link Thread#setName(String)} to catch a possible {@link Exception}s such as
     * {@link SecurityException} in sandbox environments, such as applets
     */
    private void setName(Thread thread, String name) {
        try {
            thread.setName(name);
        } catch (SecurityException se) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to set the thread name.", se);
            }
        }
    }
}
