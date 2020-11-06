package com.fileserver.app.config;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;

public class JaxbForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {

    private final ClassLoader classLoader;

    public JaxbForkJoinWorkerThreadFactory() {
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = new JaxbForkJoinWorkerThread(pool);
        thread.setContextClassLoader(classLoader);
        return thread;
    }

    private static class JaxbForkJoinWorkerThread extends ForkJoinWorkerThread {

        private JaxbForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }
    }
}