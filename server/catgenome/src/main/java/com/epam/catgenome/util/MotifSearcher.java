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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class MotifSearcher {

    private static final Map<String, String> LINKS;
    private static final Map<String, String> CODES_IUPAC;
    private static final byte OPEN_BOX_BRACE = '[';
    private static final byte CLOSED_BOX_BRACE = ']';
    private static final byte OPEN_ROUND_BRACE = '(';
    private static final byte CLOSED_ROUND_BRACE = ')';
    private static final byte CAPITAL_A = 'A';
    private static final byte LOWERCASE_A = 'a';
    private static final byte CAPITAL_T = 'T';
    private static final byte LOWERCASE_T = 't';
    private static final byte CAPITAL_G = 'G';
    private static final byte LOWERCASE_G = 'g';
    private static final byte CAPITAL_C = 'C';
    private static final byte LOWERCASE_C = 'c';

    static {
        LINKS = new HashMap<>();
        LINKS.put("R", "Y");
        LINKS.put("Y", "R");
        LINKS.put("M", "K");
        LINKS.put("K", "M");
        LINKS.put("V", "B");
        LINKS.put("B", "V");
        LINKS.put("H", "D");
        LINKS.put("D", "H");
        LINKS.put("S", "S");
        LINKS.put("W", "W");
        LINKS.put("N", "N");
        CODES_IUPAC = new HashMap<>();
        CODES_IUPAC.put("R", "[rga]");
        CODES_IUPAC.put("Y", "[ytc]");
        CODES_IUPAC.put("M", "[mac]");
        CODES_IUPAC.put("K", "[kgt]");
        CODES_IUPAC.put("S", "[sgc]");
        CODES_IUPAC.put("W", "[wat]");
        CODES_IUPAC.put("H", "[hact]");
        CODES_IUPAC.put("B", "[bgtc]");
        CODES_IUPAC.put("V", "[vgca]");
        CODES_IUPAC.put("D", "[dgat]");
        CODES_IUPAC.put("N", ".");
    }

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

    public static Map<String, String> getLinks() {
        return Collections.unmodifiableMap(new HashMap<>(LINKS));
    }

    public static Map<String, String> getCodes() {
        return Collections.unmodifiableMap(new HashMap<>(CODES_IUPAC));
    }

    public static String invertCurrentRegex(final String regex) {
        final byte[] chars = regex.getBytes();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = checkAndRevert(chars[i]);
        }
        String invertedRegex = new StringBuilder(new String(chars)).reverse().toString();
        final Map<String, String> links = getLinks();
        final Map<String, String> codes = getCodes();
        for (Map.Entry<String, String> entry : links.entrySet()) {
            if (invertedRegex.contains(entry.getKey())) {
                invertedRegex = invertedRegex.replaceAll(entry.getKey(), codes.get(entry.getValue()));
            }
        }
        return invertedRegex.toLowerCase(Locale.ROOT);
    }

    private static byte checkAndRevert(final byte value) {
        switch (value) {
            case OPEN_BOX_BRACE:
                return CLOSED_BOX_BRACE;
            case CLOSED_BOX_BRACE:
                return OPEN_BOX_BRACE;
            case OPEN_ROUND_BRACE:
                return CLOSED_ROUND_BRACE;
            case CLOSED_ROUND_BRACE:
                return OPEN_ROUND_BRACE;
            case CAPITAL_A:
            case LOWERCASE_A:
            case CAPITAL_T:
            case LOWERCASE_T:
            case CAPITAL_G:
            case LOWERCASE_G:
            case CAPITAL_C:
            case LOWERCASE_C:
                return MotifSearchIterator.complement(value);
            default:
                return value;
        }
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
        final Pattern pattern = Pattern.compile("^[\\w\\[\\]()|.]+$");
        return pattern.matcher(regex).matches();
    }
}
