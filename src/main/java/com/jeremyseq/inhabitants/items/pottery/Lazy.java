package com.jeremyseq.inhabitants.items.pottery;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {

    private final Supplier<T> delegate;
    private T value;

    public Lazy(Supplier<T> delegate) {

        this.delegate = delegate;
    }

    @Override
    public T get() {

        if (this.value == null) {

            this.value = this.delegate.get();
        }

        return this.value;
    }
}
