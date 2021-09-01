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

package com.epam.catgenome.manager.reference;

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.StrandedSequence;
import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@Slf4j
public class MotifSearchManager {

    private static final int TRACK_LENGTH = 100;
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MOTIF_LENGTH = 1000;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    public Track<StrandedSequence> fillTrackWithMotifSearch(final Track<StrandedSequence> track,
                                                            final String motif,
                                                            final StrandSerializable strand) {
        final List<StrandedSequence> result = search(
                MotifSearchRequest.builder()
                        .startPosition(track.getStartIndex())
                        .endPosition(track.getEndIndex())
                        .chromosomeId(track.getChromosome().getId())
                        .motif(motif)
                        .searchType(MotifSearchType.REGION)
                        .pageSize(Integer.MAX_VALUE)
                        .strand(strand)
                        .build()).getResult().stream()
                .map(m -> new StrandedSequence(m.getStart(), m.getEnd(), m.getValue(), m.getStrand()))
                .collect(Collectors.toList());
        track.setBlocks(result);
        return track;
    }

    public MotifSearchResult search(final MotifSearchRequest request) {
        verifyMotifSearchRequest(request);
        switch (request.getSearchType()) {
            case WHOLE_GENOME:
                return searchWholeGenomeMotifs(request);
            case CHROMOSOME:
                return searchChromosomeMotifs(request);
            case REGION:
                return searchRegionMotifs(request);
            default:
                throw new IllegalStateException("Unexpected search type: " + request.getSearchType());
        }
    }

    private void verifyMotifSearchRequest(final MotifSearchRequest request) {
        Assert.notNull(request.getSearchType(), getMessage("Search type is empty!"));
        Assert.notNull(request.getMotif(), getMessage("Motif is empty!"));
        Assert.notNull(request.getReferenceId(), getMessage("Genome id is empty!"));
        final Integer start = request.getStartPosition();
        final Integer end = request.getEndPosition();
        final MotifSearchType searchType = request.getSearchType();
        if (searchType.equals(MotifSearchType.WHOLE_GENOME)) {
            return;
        }
        Assert.notNull(request.getChromosomeId(), getMessage("Chromosome not provided!"));
        if (end != null && start != null) {
            Assert.isTrue(end - start > 0,
                    getMessage("Provided end and start are not valid: " + end + " < " + start));
        }
        if (searchType.equals(MotifSearchType.CHROMOSOME)) {
            return;
        }
        Assert.notNull(start, getMessage("Start position is empty!"));
        Assert.notNull(end, getMessage("End position is empty!"));
    }

    private MotifSearchResult searchRegionMotifs(final MotifSearchRequest request) {
        final Chromosome chromosome = loadChrById(request.getReferenceId(), request.getChromosomeId());
        return MotifSearchResult.builder()
                .result(
                    fillMotifList(
                        chromosome, request.getStartPosition(),
                        request.getEndPosition(), 0,
                            request.getStrand()))
                .chromosomeId(request.getChromosomeId())
                .pageSize(request.getPageSize())
                .position(request.getEndPosition())
                .build();
    }

    private MotifSearchResult searchChromosomeMotifs(final MotifSearchRequest request) {
        final Chromosome chromosome = loadChrById(request.getReferenceId(), request.getChromosomeId());
        int start = request.getStartPosition() == null
                ? 0
                : request.getStartPosition();
        int end = request.getEndPosition() == null
                ? chromosome.getSize()
                : request.getEndPosition();
        return MotifSearchResult.builder()
                .result(
                    fillMotifList(
                        chromosome, start, end,
                        request.getPageSize(),
                            request.getStrand()))
                .chromosomeId(request.getChromosomeId())
                .pageSize(request.getPageSize())
                .position(end)
                .build();
    }

    private MotifSearchResult searchWholeGenomeMotifs(final MotifSearchRequest request) {
        final int startPageSize = request.getPageSize() == null ? DEFAULT_PAGE_SIZE : request.getPageSize();
        int start = request.getStartPosition() == null ? 0 : request.getStartPosition();
        Chromosome chromosome = loadChrById(request.getReferenceId(), request.getChromosomeId());
        long chrId = chromosome.getId();
        int end = request.getEndPosition() == null ? chromosome.getSize() : request.getEndPosition();
        if (end < chromosome.getSize()) {
            return searchRegionMotifs(
                    MotifSearchRequest.builder()
                            .referenceId(request.getReferenceId())
                            .strand(request.getStrand())
                            .pageSize(startPageSize)
                            .searchType(MotifSearchType.REGION)
                            .motif(request.getMotif())
                            .chromosomeId(chrId)
                            .startPosition(start)
                            .endPosition(end)
                            .build());
        }
        final List<Chromosome> chromosomes = getChromosomesOfGenome(request.getReferenceId());
        final List<Motif> motifs = new ArrayList<>();
        int pageSize = startPageSize;
        while (chromosome != null) {
            chrId = chromosome.getId();
            end = chromosome.getSize();
            List<Motif> result = searchChromosomeMotifs(
                    MotifSearchRequest.builder()
                            .referenceId(request.getReferenceId())
                            .strand(request.getStrand())
                            .pageSize(pageSize)
                            .searchType(MotifSearchType.CHROMOSOME)
                            .motif(request.getMotif())
                            .chromosomeId(chrId)
                            .startPosition(start)
                            .endPosition(end)
                            .build())
                    .getResult();
            motifs.addAll(result.stream().limit(pageSize).collect(Collectors.toList()));
            pageSize -= result.size();
            if (pageSize <= 0) {
                end = motifs.get(motifs.size() - 1).getEnd();
                break;
            }
            start = 0;
            chromosome = getNextChromosome(chromosomes, chromosome);
        }
        return MotifSearchResult.builder()
                .result(motifs)
                .pageSize(startPageSize)
                .chromosomeId(chrId)
                .position(end)
                .build();
    }

    private Chromosome getNextChromosome(final List<Chromosome> chromosomes, final Chromosome chromosome) {
        for (int i = 0; i < chromosomes.size() - 1; i++) {
            if (chromosomes.get(i).getName().equals(chromosome.getName())) {
                return chromosomes.get(i + 1);
            }
        }
        return null;
    }

    private Chromosome loadChrById(final Long referenceId, final Long chromosomeId) {
        if (chromosomeId == null) {
            return getFirstChromosomeFromGenome(referenceId);
        } else {
            return referenceGenomeManager.loadChromosome(chromosomeId);
        }
    }

    private Chromosome getFirstChromosomeFromGenome(final Long referenceId) {
        return referenceGenomeManager.loadChromosomes(referenceId).get(0);
    }

    private List<Chromosome> getChromosomesOfGenome(final Long referenceId) {
        return referenceGenomeManager.loadChromosomes(referenceId);
    }

    private List<Motif> fillMotifList(final Chromosome chromosome,
                                      final Integer trackStart, final Integer trackEnd,
                                      final Integer pageSize, final StrandSerializable strand) {
        final List<Motif> motifs = new ArrayList<>();
        if (pageSize == null || pageSize == 0) {
            motifs.addAll(getStubMotifList(chromosome, trackStart, trackEnd, strand));
        } else {
            List<Motif> motifList = getStubMotifList(chromosome, trackStart, trackEnd, strand);
            if (motifList.size() > pageSize) {
                motifs.addAll(motifList.stream().limit(pageSize).collect(Collectors.toList()));
            } else {
                motifs.addAll(motifList);
            }
        }
        return motifs;
    }

    private List<Motif> getStubMotifList(final Chromosome chromosome, final Integer trackStart,
                                         final Integer trackEnd, final StrandSerializable strand) {
        final int motifStart = trackStart == null ? 0 : trackStart;
        final int motifEnd = trackEnd == null ? (motifStart + MOTIF_LENGTH) : trackEnd;
        int count = (motifEnd - motifStart) / TRACK_LENGTH;
        List<Motif> motifs = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            int start = (motifStart + TRACK_LENGTH * i);
            String value = generateString();
            int end = start + value.length();
            motifs.add(
                    new Motif(chromosome.getName(), start, end,
                            strand != null ? strand : StrandSerializable.POSITIVE,
                            value));
        }
        return motifs;
    }

    private String generateString() {
        String characters = "ATCG";
        Random rng = new Random();
        int length = rng.nextInt(TRACK_LENGTH);
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
}
