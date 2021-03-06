package ru.ifmo.rain.shelepov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.min;


public class IterativeParallelism implements ListIP {

    private ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T> List<Stream<? extends T>> packItems(List<? extends T> items, int number) {
        int basketSize = items.size() / number;
        int left = items.size() % number;

        List<Stream<? extends T>> pack = new ArrayList<>();
        int l = 0, r = 0;
        for (int i = 0; i < number; i++) {
            l = r;
            r = l + basketSize;
            if (i < left) {
                r++;
            }

            pack.add(items.subList(l, r).stream());
        }

        return pack;
    }

    private <R, T> R runTask(int threads, List<? extends T> items,
                             Function<? super Stream<? extends T>, ? extends R> functor,
                             Function<? super Stream<? extends R>, ? extends R> collector) throws InterruptedException {

        threads = max(1, min(threads, items.size()));
        List<Stream<? extends T>> packedItems = packItems(items, threads);
        List<R> result;
        List<Thread> workers = new ArrayList<>();

        if (mapper != null) {
            result = mapper.map(functor, packedItems);
        } else {
            result = new ArrayList<>(Collections.nCopies(threads, null));
            for (int i = 0; i < threads; i++) {
                final int index = i;
                Thread worker = new Thread(() -> result.set(index, functor.apply(packedItems.get(index))));
                worker.start();
                workers.add(worker);
            }
        }


        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                System.out.println("Couldn't join all threads");
            }
        }

        return collector.apply(result.stream());
    }

    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("List of values is empty");
        }

        Function<? super Stream<? extends T>, ? extends T> functor = stream -> stream.max(comparator).get();
        return runTask(threads, values, functor, functor);
    }

    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runTask(threads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(b -> (b == true)));
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return runTask(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runTask(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return runTask(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }
}
