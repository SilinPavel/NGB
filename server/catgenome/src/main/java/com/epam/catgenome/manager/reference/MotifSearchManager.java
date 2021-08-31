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
                        .pageSize(0)
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
        final Integer start = request.getStartPosition();
        final Integer end = request.getEndPosition();
        final MotifSearchType searchType = request.getSearchType();
        if (searchType.equals(MotifSearchType.REGION)) {
            Assert.notNull(request.getChromosomeId(), getMessage("Chromosome not provided!"));
            Assert.notNull(start, getMessage("Start position is empty!"));
            Assert.notNull(end, getMessage("End position is empty!"));
            Assert.isTrue(end - start > 0,
                    getMessage("Provided end and start are not valid: " + end + " < " + start));
        } else if (searchType.equals(MotifSearchType.CHROMOSOME)) {
            Assert.notNull(request.getChromosomeId(), getMessage("Chromosome not provided!"));
            if (end != null && start != null) {
                Assert.isTrue(end - start > 0,
                        getMessage("Provided end and start are not valid: " + end + " < " + start));
            }
        } else if (searchType.equals(MotifSearchType.WHOLE_GENOME)) {
            Assert.notNull(request.getReferenceId(), getMessage("Genome id is empty!"));
        }
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
        MotifSearchRequest currentRequest;
        int pageSize = request.getPageSize() == null ? 0 : request.getPageSize();
        int start = request.getStartPosition() == null ? 0 : request.getStartPosition();
        final int end = request.getEndPosition() == null ? 0 : request.getEndPosition();
        final Chromosome chromosome = getFirstChromosomeFromGenome(request.getReferenceId());
        if (end != 0 && end < chromosome.getSize()) {
            currentRequest = buildNewMotifSearchRequest(request, MotifSearchType.REGION,
                    chromosome.getId(), start, end, pageSize);
            final List<Motif> result = search(currentRequest).getResult();
            return builtNewMotifSearchResult(pageSize != 0 && result.size() > pageSize ?
                            result.stream().limit(pageSize).collect(Collectors.toList()) :
                            result,
                    pageSize, chromosome.getId(), end);
        }
        final List<Chromosome> chromosomes = getChromosomesOfGenome(request.getReferenceId());
        final List<Motif> motifs = new ArrayList<>();
        int chrEnd = 0;
        long chrId = 0;
        for (Chromosome chr : chromosomes) {
            chrEnd += chr.getSize();
            chrId = chr.getId();
            if (end != 0 && end <= chrEnd) {
                currentRequest = buildNewMotifSearchRequest(request, MotifSearchType.CHROMOSOME,
                        chrId, start, end, pageSize);
            } else {
                currentRequest = buildNewMotifSearchRequest(request, MotifSearchType.CHROMOSOME,
                        chrId, start, chrEnd, pageSize);
            }
            List<Motif> result = search(currentRequest).getResult();
            if (pageSize != 0 && result.size() >= pageSize) {
                motifs.addAll(result.stream().limit(pageSize).collect(Collectors.toList()));
                break;
            } else {
                motifs.addAll(result);
                if (pageSize != 0) {
                    pageSize -= result.size();
                }
            }
            start += chr.getSize();
            if (end != 0 && start >= end) {
                break;
            }
        }
        return builtNewMotifSearchResult(motifs, pageSize, chrId, end == 0 ? chrEnd : end);
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

    private MotifSearchResult builtNewMotifSearchResult(final List<Motif> result,
                                                        final int pageSize,
                                                        final Long chrId,
                                                        final int end) {
        return MotifSearchResult.builder()
                .result(result)
                .pageSize(pageSize)
                .chromosomeId(chrId)
                .position(end)
                .build();
    }

    private MotifSearchRequest buildNewMotifSearchRequest(final MotifSearchRequest request,
                                                          final MotifSearchType searchType,
                                                          final Long chrId,
                                                          final int start,
                                                          final int end,
                                                          final int pageSize) {
        return MotifSearchRequest.builder()
                .referenceId(request.getReferenceId())
                .strand(request.getStrand())
                .pageSize(pageSize)
                .searchType(searchType)
                .motif(request.getMotif())
                .chromosomeId(chrId)
                .startPosition(start)
                .endPosition(end)
                .build();
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
        int motifLength = TRACK_LENGTH * 10;
        final int motifEnd = trackEnd == null ? (motifStart + motifLength) : trackEnd;
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
