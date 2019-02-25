package ru.ifmo.rain.shelepov.arrayset;
import java.util.*;

import static java.lang.Math.abs;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final Comparator<? super E> comparator;
    private final List<E> data;

    public ArraySet() {
        comparator = null;
        data = Collections.emptyList();
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comp) {
        comparator = comp;
        TreeSet<E> set = new TreeSet<>(comp);
        set.addAll(collection);
        data = new ArrayList<>(set);
    }

    public ArraySet(List<E> list, Comparator<? super E> comp) {
        comparator = comp;
        data = list;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    private E get(int index) {
        if (index >= data.size() || index < 0) {
            return null;
        }

        return data.get(index);
    }

    private int binarySearch(E e) {
        return Collections.binarySearch(data, e, comparator);
    }

    private boolean validIndex(int index) {
        return index >= 0 && index < data.size();
    }

    private int find(E e, int foundShift, int notFoundShift) {
        int ind = binarySearch(e);
        if (ind >= 0) {
            ind += foundShift;
            return validIndex(ind) ? ind : -1;
        } else {
            ind = abs(ind) - 1 + notFoundShift;
            return validIndex(ind) ? ind : -1;
        }
    }

    private int lowerIndex(E e) {
        return find(e, -1, -1);
    }

    private int floorIndex(E e) {
        return find(e, 0, -1);
    }

    private int ceilingIndex(E e) {
        return find(e, 0, 0);
    }

    private int higherIndex(E e) {
        return find(e, 1, 0);
    }

    @Override
    public E lower(E e) {
        return get(lowerIndex(e));
    }

    @Override
    public E floor(E e) {
        return get(floorIndex(e));

    }

    @Override
    public E ceiling(E e) {
        return get(ceilingIndex(e));
    }

    @Override
    public E higher(E e) {
        return get(higherIndex(e));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("Set is unmodifiable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("Set is unmodifiable");
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedListView<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    private boolean invalidBounds(int l, int r) {
        return (l < 0 || r < 0 || l >= data.size() || r >= data.size() || r < l);
    }


    @SuppressWarnings("unchecked")
    private int compare(E a, E b) {
        if (comparator == null) {
            return ((Comparable)a).compareTo(b);
        } else {
            return comparator.compare(a, b);
        }
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        int l = fromInclusive ? ceilingIndex(fromElement) : higherIndex(fromElement);
        int r = toInclusive ? floorIndex(toElement) : lowerIndex(toElement);
        if (invalidBounds(l, r)) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }
        return new ArraySet<>(data.subList(l, r + 1), comparator);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        try {
            return subSet(first(), true, toElement, inclusive);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        try {
            return subSet(fromElement, inclusive, last(), true);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    private void checkEmptiness() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E first() {
        checkEmptiness();
        return data.get(0);
    }

    @Override
    public E last() {
        checkEmptiness();
        return data.get(data.size() - 1);
    }

    @Override
    public Object[] toArray() {
        return data.toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {

        return (o != null && binarySearch((E) o) >= 0);
    }

}