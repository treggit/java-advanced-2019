package ru.ifmo.rain.shelepov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

import static java.lang.Math.max;

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threadPool;
    private final Queue<Runnable> queue;
    private static final int MAX_QUEUE_SIZE = 1000000;

    private class TasksFuture<R> {
        private List<R> result;
        private int cnt = 0;

        public TasksFuture(List<R> result) {
            this.result = result;
        }

        public void setResult(int index, R val) {
            result.set(index, val);
            synchronized (this) {
                cnt++;
                if (cnt == result.size()) {
                    notify();
                }
            }
        }

        public synchronized List<R> getResult() throws InterruptedException {
            while (cnt < result.size()) {
                wait();
            }

            return result;
        }

    }

    private void calculateTask() throws InterruptedException {
        Runnable task;

        synchronized (queue) {
            while (queue.isEmpty()) {
                queue.wait();
            }

            task = queue.poll();
            queue.notifyAll();
        }

        task.run();
    }

    private void addTask(Runnable task) throws InterruptedException {
        synchronized (queue) {
            while (queue.size() > MAX_QUEUE_SIZE) {
                queue.wait();
            }

            queue.add(task);
            queue.notifyAll();
        }

    }

    private Thread createWorker() {
        return new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    calculateTask();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public ParallelMapperImpl(int threads) {
        threads = max(1, threads);
        threadPool = new ArrayList<>();
        queue = new ArrayDeque<>();

        for (int i = 0; i < threads; i++) {
            Thread thread = createWorker();
            threadPool.add(thread);
            thread.start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        TasksFuture<R> tasksFuture = new TasksFuture<>(new ArrayList<>(Collections.nCopies(args.size(), null)));
        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            addTask(() -> tasksFuture.setResult(index, f.apply(args.get(index))));
        }

        return tasksFuture.getResult();
    }

    @Override
    public void close() {
        killPool();
    }

    private void killPool() {
        threadPool.forEach(Thread::interrupt);

        for (Thread thread : threadPool) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {

            }
        }
    }
}
