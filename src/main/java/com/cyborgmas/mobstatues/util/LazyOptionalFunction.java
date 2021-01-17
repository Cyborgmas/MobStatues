package com.cyborgmas.mobstatues.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class LazyOptionalFunction<I, R> {
    private Supplier<Optional<R>> value = Optional::empty;
    private final Function<I, Optional<R>> getter;
    private final R defaultResult;
    private R result;
    private boolean applied = false;

    public LazyOptionalFunction(Function<I, Optional<R>> getter) {
        this(getter, null);
    }

    public LazyOptionalFunction(Function<I, Optional<R>> getter, R defaultResult) {
        this.getter = getter;
        this.defaultResult = defaultResult;
    }

    public void apply(I in) {
        this.applied = true;
        this.value = () -> getter.apply(in);
    }

    public R get() {
        if (this.result == null && applied) {
            this.result = value.get().orElse(defaultResult);
        }
        return applied ? result : defaultResult;
    }
}
