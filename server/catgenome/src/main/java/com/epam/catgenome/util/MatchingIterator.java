package com.epam.catgenome.util;

import lombok.Value;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class MatchingIterator implements Iterator<MatchingIterator.MatchingResult> {

    private final List<CheckableMatcher> matchers;

    public MatchingIterator(final Matcher... matchers) {
        this.matchers = Arrays.stream(matchers)
                .map(CheckableMatcher::new)
                .collect(Collectors.toList());
        enumerateMatchers(this.matchers);
    }

    @Override
    public boolean hasNext() {
        return matchers.stream().anyMatch(CheckableMatcher::hasNext);
    }

    @Override
    public MatchingResult next() {
        final CheckableMatcher checkableMatcher = matchers.stream()
                .filter(CheckableMatcher::hasNext)
                .min(Comparator.comparing(CheckableMatcher::start))
                .orElseThrow(()->new NoSuchElementException("\"next()\" invoked without invoking \"hasNext()\"!"));
        checkableMatcher.find();
        return new MatchingResult(checkableMatcher.matcherNumber,
                checkableMatcher.start(),
                checkableMatcher.end()
        );
    }

    private void enumerateMatchers(final List<CheckableMatcher> matchers) {
        AtomicInteger counter = new AtomicInteger(0);
        matchers.forEach(m -> m.matcherNumber = counter.getAndIncrement());
    }

    private static class CheckableMatcher {

        private final Matcher matcher;
        private boolean nextValueAvailable;
        private boolean hasNextInvoked;
        private int currentPosition;
        private int matcherNumber;



        public CheckableMatcher(final Matcher matcher) {
            this.matcher = matcher;
        }

        public boolean hasNext() {
            if (!hasNextInvoked) {
                nextValueAvailable = matcher.find(currentPosition);
                hasNextInvoked = true;
                if (nextValueAvailable) {
                    currentPosition = matcher.start() + 1;
                }
            }
            return nextValueAvailable;
        }

        public boolean find() {
            if (!hasNextInvoked) {
                throw new IllegalStateException("\"find()\" invoked without invoking \"hasNext()\"!");
            }
            hasNextInvoked = false;
            return nextValueAvailable;
        }

        public int start() {
            return matcher.start();
        }

        public int end() {
            return matcher.end();
        }
    }

    @Value
    public static class MatchingResult {
        Integer matcherNumber;
        Integer matchingStartResult;
        Integer matchingEndResult;
    }
}
