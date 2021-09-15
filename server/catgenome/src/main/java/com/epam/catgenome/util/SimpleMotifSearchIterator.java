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

import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import lombok.Value;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleMotifSearchIterator implements Iterator<Motif>  {

    private final Deque<Match> positiveMatches;
    private final Deque<Match> negativeMatches;
    private final String contig;
    private final byte[] sequence;
    private final int offset;
    private final boolean includeSequence;

    public SimpleMotifSearchIterator(final byte[] seq, final String iupacRegex,
                                     final StrandSerializable strand, final String contig,
                                     final int start, final boolean includeSequence) {
        if (strand != null && strand != StrandSerializable.POSITIVE && strand != StrandSerializable.NEGATIVE) {
            throw new IllegalStateException("Not supported strand: " + strand);
        }
        this.contig = contig;
        this.sequence = seq;
        this.offset = start;
        this.includeSequence = includeSequence;

        String invertedRegex = invertCurrentRegex(iupacRegex);
        final Pattern patternPositive =
                Pattern.compile(MotifSearcher.convertIupacToRegex(iupacRegex), Pattern.CASE_INSENSITIVE);
        final Pattern patternNegative =
                Pattern.compile(MotifSearcher.convertIupacToRegex(invertedRegex), Pattern.CASE_INSENSITIVE);
        if (strand == null) {
            this.positiveMatches = populatePositiveMatches(patternPositive.matcher(new String(seq)));
            this.negativeMatches = populatePositiveMatches(patternNegative.matcher(new String(seq)));
        } else if (strand == StrandSerializable.POSITIVE) {
            this.positiveMatches = populatePositiveMatches(patternPositive.matcher(new String(seq)));
            this.negativeMatches = new LinkedList<>();
        } else {
            this.positiveMatches = new LinkedList<>();
            this.negativeMatches = populatePositiveMatches(patternNegative.matcher(new String(seq)));
        }
    }

    private String invertCurrentRegex(final String regex) {
        final byte[] chars = regex.getBytes();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = checkAndRevert(chars[i]);
        }
        return new StringBuilder(new String(chars)).reverse().toString();
    }

    private byte checkAndRevert(final byte value) {
        final byte openBoxBrace = '[';
        final byte closedBoxBrace = ']';
        final byte openRoundBrace = '(';
        final byte closedRoundBrace = ')';
        final byte level = '|';
        final byte dot = '.';
        if (value == openBoxBrace || value == closedBoxBrace
                || value == openRoundBrace || value == closedRoundBrace
                || value == level || value == dot) {
            switch (value) {
                case openBoxBrace:
                    return closedBoxBrace;
                case closedBoxBrace:
                    return openBoxBrace;
                case openRoundBrace:
                    return closedRoundBrace;
                case closedRoundBrace:
                    return openRoundBrace;
                default:
                    return value;
            }
        } else {
            return MotifSearchIterator.complement(value);
        }
    }

    private Deque<Match> populatePositiveMatches(final Matcher matcher) {
        int position = 0;
        LinkedList<Match> matches = new LinkedList<>();
        while (matcher.find(position)) {
            matches.add(new Match(matcher.start(), matcher.end() - 1));
            position = matcher.start() + 1;
        }
        return matches;
    }

    @Override
    public boolean hasNext() {
        return !positiveMatches.isEmpty() || !negativeMatches.isEmpty();
    }

    @Override
    public Motif next() {
        Match match;
        StrandSerializable currentStrand;
        if (negativeMatches.isEmpty()) {
            match = positiveMatches.removeFirst();
            currentStrand = StrandSerializable.POSITIVE;
        } else if (positiveMatches.isEmpty() || negativeMatches.peekLast().start < positiveMatches.peekFirst().start) {
            match = negativeMatches.removeLast();
            currentStrand = StrandSerializable.NEGATIVE;
        } else {
            match = positiveMatches.removeFirst();
            currentStrand = StrandSerializable.POSITIVE;
        }
        if (includeSequence) {
            return getMotif(contig, match.start, match.end, currentStrand);
        }
        return new Motif(contig, match.start + offset, match.end + offset, currentStrand, null);
    }

    private Motif getMotif(final String contig, final int start, final int end, StrandSerializable strand) {
        final StringBuilder result = new StringBuilder();
        for (int i = start; i <= end; i++) {
            result.append((char) sequence[i]);
        }
        return new Motif(contig, start + offset, end + offset, strand, result.toString());
    }

    @Value
    public static class Match {
        Integer start;
        Integer end;
    }
}
