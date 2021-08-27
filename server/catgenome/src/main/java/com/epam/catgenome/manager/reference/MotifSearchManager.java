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

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private ReferenceManager referenceManager;

    private final int TRACK_LENGTH = 100;

    public Track<StrandedSequence> fillTrackWithMotifSearch(final Track<StrandedSequence> track,
                                                            final String motif) {
        verifyInputTrackAndMotif(track, motif);
        final List<StrandedSequence> result = fillStrandedSequenceList(track.getStartIndex(),
                track.getEndIndex(), track.getChromosome().getId(), motif);
        track.setBlocks(result);
        return track;
    }

    private List<StrandedSequence> fillStrandedSequenceList(final Integer trackStart, final Integer trackEnd,
                                                            final Long chromosomeId, final String motif) {
        final List<Motif> motifs = getMotifList(trackStart, trackEnd, motif, chromosomeId);
        return motifs.stream()
                .map(m -> new StrandedSequence(m.getStart(), m.getEnd(), m.getValue(), m.getStrand()))
                .collect(Collectors.toList());
    }

    private void verifyInputTrackAndMotif(final Track<StrandedSequence> track, final String motif) {
        Assert.notNull(motif, getMessage("Motif is empty!"));
        Assert.notNull(track.getChromosome().getId(), getMessage("Chromosome not found!"));
        Assert.notNull(track.getStartIndex(), getMessage("Start index is empty!"));
        Assert.notNull(track.getEndIndex(), getMessage("End index is empty!"));
        Assert.isTrue(track.getEndIndex() - track.getStartIndex() > 0,
                getMessage("Wrong indexes!"));
    }

    public MotifSearchResult getMotifSearchResultByRequest(final MotifSearchRequest motifSearchRequest) {
        return createMotifSearchResult(motifSearchRequest);
    }

    private MotifSearchResult createMotifSearchResult(final MotifSearchRequest motifSearchRequest) {
        verifyMotifSearchRequest(motifSearchRequest);
        return search(motifSearchRequest);
    }

    public MotifSearchResult search(final MotifSearchRequest request) {
        switch (request.getSearchType()) {
            case WHOLE_GENOME:
                return searchWholeGenomeMotifs(request);
            case CHROMOSOME:
                return searchChromosomeMotifs(request);
            case REGION:
                return searchRegionMotifs(request);
        }
        throw new IllegalArgumentException();
    }

    private MotifSearchResult searchRegionMotifs(final MotifSearchRequest motifSearchRequest) {
        return MotifSearchResult.builder()
                .result(fillMotifList(motifSearchRequest.getStartPosition(),
                        motifSearchRequest.getEndPosition(),
                        0,
                        motifSearchRequest.getMotif(),
                        motifSearchRequest.getChromosomeId()))
                .chromosomeId(motifSearchRequest.getChromosomeId())
                .pageSize(motifSearchRequest.getPageSize())
                .position(motifSearchRequest.getEndPosition())
                .build();
    }

    private MotifSearchResult searchChromosomeMotifs(final MotifSearchRequest motifSearchRequest) {
        return MotifSearchResult.builder()
                .result(fillMotifList(motifSearchRequest.getStartPosition(),
                        motifSearchRequest.getEndPosition(),
                        motifSearchRequest.getPageSize(),
                        motifSearchRequest.getMotif(),
                        motifSearchRequest.getChromosomeId()))
                .chromosomeId(motifSearchRequest.getChromosomeId())
                .pageSize(motifSearchRequest.getPageSize())
                .position(motifSearchRequest.getEndPosition())
                .build();
    }

    private MotifSearchResult searchWholeGenomeMotifs(final MotifSearchRequest motifSearchRequest) {
        return searchChromosomeMotifs(motifSearchRequest);
    }

    private void verifyMotifSearchRequest(final MotifSearchRequest motifSearchRequest) {
        Assert.notNull(motifSearchRequest.getMotif(), getMessage("Motif is empty!"));
        final Integer start = motifSearchRequest.getStartPosition();
        final Integer end = motifSearchRequest.getEndPosition();
        final MotifSearchType searchType = motifSearchRequest.getSearchType();
        if (searchType.equals(MotifSearchType.REGION)) {
            Assert.notNull(start, getMessage("Start position is empty!"));
            Assert.notNull(end, getMessage("End position is empty!"));
            Assert.isTrue(end - start > 0, getMessage("Wrong indexes!"));
        } else if (searchType.equals(MotifSearchType.CHROMOSOME)) {
            Assert.notNull(motifSearchRequest.getChromosomeId(), getMessage("Chromosome not found!"));
            Assert.notNull(start, getMessage("Start position is empty!"));
            if (end != null) {
                Assert.isTrue(end - start > 0, getMessage("Wrong indexes!"));
            }
        } else if (searchType.equals(MotifSearchType.WHOLE_GENOME)) {
            Assert.notNull(motifSearchRequest.getReferenceId(), getMessage("Genome id is empty!"));
        }
    }

    private List<Motif> fillMotifList(final Integer trackStart, final Integer trackEnd,
                                      final Integer pageSize, final String motif,
                                      final Long chromosomeId) {
        final List<Motif> motifs = new ArrayList<>();
        if (pageSize == 0 || pageSize < (trackEnd - trackStart)) {
            motifs.addAll(getMotifList(trackStart, trackEnd, motif, chromosomeId));
        } else {
            int length = (trackEnd - trackStart) / pageSize + ((trackEnd - trackStart) % pageSize);
            int start = trackStart;
            int end = trackStart + length;
            for (int i = 0; i < pageSize; i++) {
                motifs.addAll(getMotifList(start, end, motif, chromosomeId));
                start += length;
                end += length;
            }
        }
        return motifs;
    }

    private List<Motif> getMotifList(final Integer trackStart, final Integer trackEnd,
                                     final String motif, final Long chromosomeId) {
        return getStubMotifList(trackStart, trackEnd, chromosomeId);
    }

    private List<Motif> getStubMotifList(final Integer trackStart, final Integer trackEnd,
                                         final Long chromosomeId) {
        String chrName;
        try {
            Chromosome chr = referenceGenomeManager.loadChromosome(chromosomeId);
            chrName = chr.getName();
        } catch (Exception e) {
            chrName = "chr" + (chromosomeId + 1);
        }
        final int motifStart = trackStart == null ? 0 : trackStart;
        final int motifEnd = trackEnd == null ? (motifStart + 1000) : trackEnd;
        int count = (motifEnd - motifStart) / TRACK_LENGTH;
        List<Motif> motifs = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            int start = (motifStart + TRACK_LENGTH * i);
            int end = (start + TRACK_LENGTH) - 1;
            Motif curMotif = new Motif(chrName, start, end, StrandSerializable.POSITIVE, generateString());
            motifs.add(curMotif);
        }
        return motifs;
    }

    private String generateString() {
        String characters = "ATCG";
        Random rng = new java.util.Random();
        int length = rng.nextInt(TRACK_LENGTH);
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
}
