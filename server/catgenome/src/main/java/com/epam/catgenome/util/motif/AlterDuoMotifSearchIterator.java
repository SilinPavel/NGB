package com.epam.catgenome.util.motif;

import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlterDuoMotifSearchIterator implements Iterator<Motif> {

    private final Matcher matcherPos;
    private final Matcher matcherNeg;
    private int currentStartPositionPos;
    private int currentStartPositionNeg;
    private final String contig;
    private final int offset;
    private final boolean includeSequence;

    private boolean posFlag;
    private boolean negFlag;

    public AlterDuoMotifSearchIterator(final byte[] seq, final String iupacRegex, final String contig,
                                       final int start, final boolean includeSequence) {

        final String sequence = new String(seq);
        matcherPos = Pattern.compile(IupacRegexConverter.convertIupacToRegex(iupacRegex), Pattern.CASE_INSENSITIVE)
                .matcher(sequence);
        matcherNeg = Pattern.compile(
                        IupacRegexConverter.convertIupacToComplementReversedRegex(iupacRegex),
                        Pattern.CASE_INSENSITIVE)
                .matcher(sequence);
        this.contig = contig;
        this.offset = start;
        this.includeSequence = includeSequence;
    }

    @Override
    public boolean hasNext() {
        if (!posFlag) {
            posFlag = matcherPos.find(currentStartPositionPos);
        }
        if (!negFlag) {
            negFlag = matcherNeg.find(currentStartPositionNeg);
        }
        return negFlag || posFlag;
    }

    @Override
    public Motif next() {
        if (!negFlag || posFlag && matcherPos.start() <= matcherNeg.start()) {
            currentStartPositionPos = matcherPos.start() + 1;
            posFlag = false;
            return new Motif(contig, matcherPos.start() + offset,
                    matcherPos.end() - 1 + offset, StrandSerializable.POSITIVE,
                    includeSequence ? matcherPos.group() : null);
        }
        currentStartPositionNeg = matcherNeg.start() + 1;
        negFlag = false;
        return new Motif(contig, matcherNeg.start() + offset,
                matcherNeg.end() - 1 + offset, StrandSerializable.NEGATIVE,
                includeSequence ? matcherNeg.group() : null);
    }
}