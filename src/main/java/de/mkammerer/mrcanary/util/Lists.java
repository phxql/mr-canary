package de.mkammerer.mrcanary.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class Lists {
    private Lists() {
    }

    public static <I, O> List<O> map(Collection<I> in, Function<I, O> mapper) {
        List<O> result = new ArrayList<>(in.size());
        for (I i : in) {
            result.add(mapper.apply(i));
        }
        return result;
    }
}
