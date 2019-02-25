package ru.ifmo.rain.shelepov.arrayset;

import java.util.*;

public class ReversedListView<E> extends AbstractList<E> {
    private final List<E> data;
    private boolean reversed;

    ReversedListView(ReversedListView<E> other) {
        this.data = other.data;
        this.reversed = !other.reversed;
    }

    ReversedListView(List<E> other) {
        this.reversed = true;
        this.data = other;
    }

    @Override
    public E get(int ind) {
        return reversed ? (data.get(data.size() - 1 - ind)) : (data.get(ind));
    }

    @Override
    public int size() {
        return data.size();
    }


}
