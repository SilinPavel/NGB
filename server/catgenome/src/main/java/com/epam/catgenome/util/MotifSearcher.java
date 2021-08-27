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
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MotifSearcher {

    private MotifSearcher() {}

    public static List<Motif> getPartialResult(final String buf, final String regex, String contig) {

        final String negativeBuf = reverseAndComplement(buf);
        final Pattern pattern = Pattern.compile(convertIupacToRegex(regex), Pattern.CASE_INSENSITIVE);
        final Matcher positiveMatcher = pattern.matcher(buf);
        final Matcher negativeMatcher = pattern.matcher(negativeBuf);

        final MatchingIterator multiMatcher = new MatchingIterator(positiveMatcher, negativeMatcher);
        final List<Motif> motifList = new ArrayList<>();
        while (multiMatcher.hasNext()) {
            final MatchingIterator.MatchingResult matchingResult = multiMatcher.next();
            final int start = matchingResult.getMatchingStartResult();
            final int end = matchingResult.getMatchingEndResult();
            final boolean negative = matchingResult.getMatcherNumber() != 0;
            final String value = negative
                    ? negativeBuf.substring(start, end)
                    : buf.substring(start, end);
            motifList.add(new Motif(contig, start, end, negative, value));
        }
        return motifList;
    }

    /**
     * Provides reverse complement operation on sequence string
     *
     * @param sequence nucleotide sequence
     * @return reversed complement nucleotide sequence string
     */
    private static String reverseAndComplement(final String sequence) {
        final char[] chars = sequence.toCharArray();
        final char[] complementChars = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            complementChars[i] = complement(chars[i]);
        }
        return StringUtils.reverse(new String(complementChars));
    }

    /**
     * Converts specified nucleotide to complement one.
     *
     * @param nucleotide nucleotide
     * @return complement nucleotide
     */
    private static char complement(final char nucleotide) {
        char complement;
        switch (Character.toUpperCase(nucleotide)) {
            case 'A':
                complement = 'T';
                break;
            case 'T':
                complement = 'A';
                break;
            case 'G':
                complement = 'C';
                break;
            case 'C':
                complement = 'G';
                break;
            case 'N':
                complement = 'N';
                break;
            default:
                throw new IllegalArgumentException(MessageHelper.getMessage(MessagesConstants.ERROR_INVALID_NUCLEOTIDE,
                        nucleotide));
        }
        return complement;
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
            result.append(IupacRegex.getRegexByIupacLetter(regex.substring(i,i+1)));
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
