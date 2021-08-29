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

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.reference.motif.Motif;

import java.util.Spliterators;
import java.util.Spliterator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class MotifSearcher {

    private static final int NEGATIVE_STRAND = 1;
    private static final byte CAPITAL_A = 'A';
    private static final byte CAPITAL_C = 'C';
    private static final byte CAPITAL_G = 'G';
    private static final byte CAPITAL_T = 'T';
    private static final byte CAPITAL_N = 'N';
    private static final byte LOWERCASE_A = 'a';
    private static final byte LOWERCASE_C = 'c';
    private static final byte LOWERCASE_G = 'g';
    private static final byte LOWERCASE_T = 't';
    private static final byte LOWERCASE_N = 'n';

    private MotifSearcher() {}

    public static List<Motif> search(final byte[] seq, final String regex, String contig) {

        final String sequence = new String(seq);
        final String negativeSequence = reverseAndComplement(seq);
        final Pattern pattern = Pattern.compile(convertIupacToRegex(regex), Pattern.CASE_INSENSITIVE);
        final Matcher positiveMatcher = pattern.matcher(sequence);
        final Matcher negativeMatcher = pattern.matcher(negativeSequence);

        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(new MatchingIterator(positiveMatcher, negativeMatcher),
                                Spliterator.ORDERED), false)
                .map(matchingResult -> {
                    final int start = matchingResult.getMatchingStartResult();
                    final int end = matchingResult.getMatchingEndResult();
                    final boolean negative = matchingResult.getMatcherNumber() == NEGATIVE_STRAND;
                    final String value = negative
                            ? negativeSequence.substring(start, end)
                            : sequence.substring(start, end);
                    return new Motif(contig, start, end, negative, value);
                })
                .collect(Collectors.toList());
    }

    /**
     * Provides reverse complement operation on sequence string
     *
     * @param sequence nucleotide sequence
     * @return reversed complement nucleotide sequence string
     */
    private static String reverseAndComplement(final byte[] sequence) {
        final byte[] reversedSequence = new byte[sequence.length];
        for (int i = 0, j = sequence.length - 1; i < sequence.length; i++, j--) {
            reversedSequence[i] = complement(sequence[j]);
        }
        return new String(reversedSequence);
    }

    /**
     * Converts specified nucleotide to complement one.
     *
     * @param nucleotide nucleotide
     * @return complement nucleotide
     */
    public static byte complement(final byte nucleotide) {
        switch (nucleotide) {
            case CAPITAL_A:
            case LOWERCASE_A:
                return CAPITAL_T;
            case CAPITAL_C:
            case LOWERCASE_C:
                return CAPITAL_G;
            case CAPITAL_G:
            case LOWERCASE_G:
                return CAPITAL_C;
            case CAPITAL_T:
            case LOWERCASE_T:
                return CAPITAL_A;
            case CAPITAL_N:
            case LOWERCASE_N:
                return CAPITAL_N;
            default:
                throw new IllegalArgumentException(MessageHelper.getMessage(MessagesConstants.ERROR_INVALID_NUCLEOTIDE,
                        nucleotide));
        }
    }

    /**
     * Converts specified IUPAC regex to the plain nucleotide regex
     *
     * @param regex IUPAC nucleotide regex
     * @return plain nucleotide regex
     */
    public static String convertIupacToRegex(final String regex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < regex.length(); i++) {
            result.append(IupacRegex.getRegexByIupacLetter(regex.substring(i, i + 1)));
        }
        return result.toString();
    }

    /**
     *IUPAC Ambiguity codes translated into regex, using java syntax
     *Source: http://www.chem.qmul.ac.uk/iubmb/misc/naseq.html
     */
    private enum IupacRegex {

        G("g"),
        A("a"),
        T("t"),
        C("c"),
        R("[rga]"),
        Y("[ytc]"),
        M("[mac]"),
        K("[kgt]"),
        S("[sgc]"),
        W("[wat]"),
        H("[hact]"),
        B("[bgtc]"),
        V("[vgca]"),
        D("[dgat]"),
        N(".");

        private final String regex;

        IupacRegex(final String regex) {
            this.regex = regex;
        }

        private static String getRegexByIupacLetter(final String letter) {
            return Arrays.stream(values())
                    .filter(v -> v.toString().equalsIgnoreCase(letter))
                    .findFirst()
                    .map(v -> v.regex)
                    .orElseGet(() -> letter.toLowerCase(Locale.US));
        }
    }
}
