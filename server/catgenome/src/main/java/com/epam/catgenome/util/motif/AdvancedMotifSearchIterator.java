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

package com.epam.catgenome.util.motif;

import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedMotifSearchIterator implements Iterator<Motif> {

    private static final String POSITIVE_GROUP = "p";
    private static final String NEGATIVE_GROUP = "n";

    private final Matcher matcherPos;
    private final Matcher matcherNeg;
    private int currentStartPositionPos;
    private int currentStartPositionNeg;
    private final String contig;
    private final int offset;
    private final boolean includeSequence;

    private final String regexPos;
    private final String regexNeg;
    private boolean posFlag;
    private boolean negFlag;

    public AdvancedMotifSearchIterator(final byte[] seq, final String iupacRegex, final String contig,
                                       final int start, final boolean includeSequence) {
        regexPos = IupacRegexConverter.convertIupacToRegex(iupacRegex);
        regexNeg = IupacRegexConverter.convertIupacToComplementReversedRegex(iupacRegex);
        matcherPos = Pattern.compile(regexPos, Pattern.CASE_INSENSITIVE)//"(ttttatttcttttCttac)|(aaaataaagaaaaGaatg)"//IupacRegexConverter.combineIupacRegex(iupacRegex, POSITIVE_GROUP, NEGATIVE_GROUP), Pattern.CASE_INSENSITIVE)
                .matcher(new String(seq));
        matcherNeg = Pattern.compile(regexNeg, Pattern.CASE_INSENSITIVE)//"(ttttatttcttttCttac)|(aaaataaagaaaaGaatg)"//IupacRegexConverter.combineIupacRegex(iupacRegex, POSITIVE_GROUP, NEGATIVE_GROUP), Pattern.CASE_INSENSITIVE)
                .matcher(new String(seq));

        this.contig = contig;
        this.offset = start;
        this.includeSequence = includeSequence;
    }


    @Override
    public boolean hasNext() {
        posFlag = matcherPos.find(currentStartPositionPos);
        negFlag = matcherNeg.find(currentStartPositionNeg);
        return posFlag || negFlag;
    }

    @Override
    public Motif next() {
        boolean pos = false;
        if (currentStartPositionPos <= currentStartPositionNeg && posFlag) {
            currentStartPositionPos = matcherPos.start() + 1;
            pos = true;
        } else {
            currentStartPositionNeg = matcherNeg.start() + 1;
        }
        return new Motif(contig, pos ? matcherPos.start() + offset : matcherNeg.start() + offset,
                pos ? matcherPos.end() - 1 + offset : matcherNeg.end() - 1 + offset,
                getCurrentMatchStrand(),  pos ? matcherPos.group() : matcherNeg.group());
    }

    private StrandSerializable getCurrentMatchStrand() {
        return StrandSerializable.NEGATIVE;//matcher.start(POSITIVE_GROUP) != -1 ? StrandSerializable.POSITIVE : StrandSerializable.NEGATIVE;
    }
}
