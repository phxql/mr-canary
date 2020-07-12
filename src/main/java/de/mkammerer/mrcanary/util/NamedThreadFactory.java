package de.mkammerer.mrcanary.util;

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class NamedThreadFactory implements ThreadFactory {
    private final AtomicInteger threadCounter = new AtomicInteger();
    private final String namePattern;

    @Override
    public Thread newThread(@NonNull Runnable r) {
        String name = String.format(namePattern, threadCounter.getAndIncrement());
        return new Thread(r, name);
    }
}
