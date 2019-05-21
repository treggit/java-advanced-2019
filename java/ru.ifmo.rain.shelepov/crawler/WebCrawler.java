package ru.ifmo.rain.shelepov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Struct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;


public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final int perHost;

    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;

    private final Map<String, HostQueue> hostQueueMap;

    private class HostQueue {
        private int currentWorkers;
        private final Queue<Runnable> tasks;

        HostQueue() {
            currentWorkers = 0;
            tasks = new ArrayDeque<>();
        }

        private synchronized void next() {
            if (tasks.size() == 0) {
                currentWorkers--;
            } else {
                downloadersPool.submit(tasks.poll());
            }
        }

        private synchronized void add(Runnable runnable) {
            if (currentWorkers < perHost) {
                currentWorkers++;
                downloadersPool.submit(runnable);
            } else {
                tasks.add(runnable);
            }
        }
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        hostQueueMap = new ConcurrentHashMap<>();
    }

    private void downloadImpl(String url, int depth, int maxDepth, Map<String, IOException> errors, Set<String> downloaded, Set<String> next, Phaser phaser) {
        try {
            String host = URLUtils.getHost(url);
            HostQueue hostQueue = hostQueueMap.computeIfAbsent(host, str -> new HostQueue());
            Runnable task = () -> {
                try {
                    Document document = downloader.download(url);
                    downloaded.add(url);
                    if (depth + 1 < maxDepth) {
                        Runnable extraction = () -> {
                            try {
                                next.addAll(document.extractLinks());
                            } catch (IOException ignored) {
                            } finally {
                                phaser.arriveAndDeregister();
                            }
                        };
                        phaser.register();
                        extractorsPool.submit(extraction);
                    }
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    hostQueue.next();
                    phaser.arriveAndDeregister();
                }
            };
            phaser.register();
            hostQueue.add(task);
        } catch (MalformedURLException e) {
            errors.put(url, e);
        }
    }

    @Override
    public Result download(String url, int depth) {
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> processed = new HashSet<>();
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Phaser phaser = new Phaser(1);
        Set<String> layer = ConcurrentHashMap.newKeySet();
        Set<String> next = ConcurrentHashMap.newKeySet();
        layer.add(url);
        for (int i = 0; i < depth; i++) {
            final int index = i;
            layer.stream().filter(processed::add)
                    .forEach(link -> downloadImpl(link, index, depth, errors, downloaded, next, phaser));
            phaser.arriveAndAwaitAdvance();
            layer = next;
        }
        return new Result(new ArrayList<>(downloaded), errors);
    }

    @Override
    public void close() {
        downloadersPool.shutdownNow();
        extractorsPool.shutdownNow();
    }

    private static int getArgument(String[] args, int position, int defaultValue) throws NumberFormatException {
        return position < args.length ? Integer.parseInt(args[position]) : defaultValue;
    }

    public static void main(String[] args) {
        if (args == null || !(args.length >= 1 && args.length <= 5)) {
            System.err.println("Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Argument can't be null");
            return;
        }
        try {
            int depth = getArgument(args, 1, 1);
            int downloads = getArgument(args, 2, 32);
            int extractors = getArgument(args, 3, 8);
            int perHost = getArgument(args, 4, 4);
            try (var webCrawler = new WebCrawler(new CachingDownloader(), downloads, extractors, perHost)) {
                webCrawler.download(args[0], depth);
            } catch (IOException e) {
                System.err.println("Can't create CachingDownloader: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.err.println("Can't parse argument: " + e.getMessage());
        }
    }
}