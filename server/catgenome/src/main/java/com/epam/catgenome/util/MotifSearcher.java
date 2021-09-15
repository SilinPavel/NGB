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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class MotifSearcher {

    private MotifSearcher() {
    }

    public static List<Motif> search(final byte[] seq, final String regex,
                                     final String contig, final int start, final boolean includeSequence) {
        return search(seq, regex, null, contig, start, includeSequence);
    }

    public static List<Motif> search(final byte[] seq, final String regex,
                                     final StrandSerializable strand, final String contig,
                                     final int start, final boolean includeSequence) {
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(
                        getIterator(seq, regex, strand, contig, start, includeSequence),
                        Spliterator.ORDERED), false)
                .collect(Collectors.toList());
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
     * IUPAC Ambiguity codes translated into regex, using java syntax
     * Source: http://www.chem.qmul.ac.uk/iubmb/misc/naseq.html
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

    private static Iterator<Motif> getIterator(final byte[] seq, final String regex,
                                        final StrandSerializable strand, final String contig,
                                        final int start, final boolean includeSequence){
        if(isSimpleRegex(regex)){
            return new SimpleMotifSearchIterator(seq, regex, strand, contig, start, includeSequence);
        }
        return new MotifSearchIterator(seq, regex, strand, contig, start, includeSequence);
    }


    private static boolean isSimpleRegex(final String regex) {
        final Pattern pattern = Pattern.compile("^[acgtACGT\\[\\]()|.]+$");
        final Matcher matcher = pattern.matcher(regex);
        return matcher.matches();
    }
}
