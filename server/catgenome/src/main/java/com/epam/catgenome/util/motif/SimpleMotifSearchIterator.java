package com.epam.catgenome.util.motif;

import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SimpleMotifSearchIterator implements Iterator<Motif> {

    private static final int POSITIVE = 1;
    private static final int NEGATIVE = 2;
    private static final int BOTH = 3;
    private static final int MISMATCH = 0;

    private final byte[] regexpPos;
    private final byte[] regexpNeg;
    private final byte[] sequence;
    private int currentPosition;
    private int matchType;
    private final String contig;
    private final int offset;
    private final boolean includeSequence;



    public SimpleMotifSearchIterator(final byte[] seq, final String iupacRegex, final String contig,
                                     final int start, final boolean includeSequence) {
        regexpPos = iupacRegex.toLowerCase().getBytes(StandardCharsets.UTF_8);
        regexpNeg = IupacRegexConverter.convertIupacToComplementReversedRegex(iupacRegex).getBytes(StandardCharsets.UTF_8);
        sequence = seq;
        this.contig = contig;
        this.offset = start;
        this.includeSequence = includeSequence;
    }

    @Override
    public boolean hasNext() {
        while (matchType == MISMATCH) {
            matchType = compareToRegex(currentPosition);
            currentPosition++;
            if (currentPosition > (sequence.length - regexpPos.length)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Motif next() {
        switch (matchType) {

            case BOTH:
                matchType &= ~POSITIVE;
                return new Motif(contig, currentPosition + offset,
                        currentPosition + regexpPos.length - 1 + offset, StrandSerializable.POSITIVE,
                        includeSequence ? new String(regexpPos) : null);

            case POSITIVE:
                matchType = MISMATCH;
                currentPosition++;
                return new Motif(contig, currentPosition + offset - 1,
                        currentPosition + regexpPos.length - 1 + offset, StrandSerializable.POSITIVE,
                        includeSequence ? new String(regexpPos) : null);

            case NEGATIVE:
                matchType = MISMATCH;
                currentPosition++;
                return new Motif(contig, currentPosition + offset - 1,
                        currentPosition + regexpNeg.length - 1 + offset, StrandSerializable.NEGATIVE,
                        includeSequence ? new String(regexpNeg) : null);
        }
        throw new NoSuchElementException("There is not next element!");
    }

    private int compareToRegex(final int position) {

        boolean posMatches = true;
        boolean negMatches = true;
        for (int i = 0; i < regexpPos.length; i++) {
            posMatches = posMatches
                    && (sequence[position + i] == regexpPos[i] || sequence[position + i] == regexpPos[i] - 0x20);
            negMatches = negMatches
                    && (sequence[position + i] == regexpNeg[i] || sequence[position + i] == regexpNeg[i] - 0x20);
            if (!posMatches && !negMatches) {
                return MISMATCH;
            }
        }
        return (posMatches ? POSITIVE : 0) | (negMatches ? NEGATIVE : 0);
    }
}
