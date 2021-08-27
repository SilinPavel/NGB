/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.util;

import lombok.Value;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

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
