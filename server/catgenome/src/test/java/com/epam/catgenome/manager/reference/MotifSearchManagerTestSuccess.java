package com.epam.catgenome.manager.reference;

import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MotifSearchManagerTestSuccess {

    @Autowired
    private MotifSearchManager motifSearchManager;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    private static final String A3_FA_PATH = "classpath:templates/A3.fa";
    private static final String HP_GENOME_PATH = "classpath:templates/reference/hp.genome.fa";
    private static final String TEST_WG_PATH = "classpath:templates/Test_wg.fa";

    private Resource resource;
    private Reference referenceOne;
    private Reference referenceTwo;
    private long idRef;
    private long idChr;
    private long idRefTwo;
    private long idChrTwo;

    @Before
    public void registerFileOne() throws IOException {
        resource = context.getResource(A3_FA_PATH);
        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("A3.fa");
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);
        referenceOne = referenceManager.registerGenome(request);
        idRef = referenceOne.getId();
        idChr = referenceOne.getChromosomes().get(0).getId();
    }

    @Before
    public void registerFileTwo() throws IOException {
        resource = context.getResource(HP_GENOME_PATH);
        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("hp.genome.fa");
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);
        referenceTwo = referenceManager.registerGenome(request);
        idRefTwo = referenceTwo.getId();
        idChrTwo = referenceTwo.getChromosomes().get(1).getId();
    }

    @After
    public void unregisterFiles() throws IOException {
        referenceManager.unregisterGenome(referenceOne.getId());
        referenceManager.unregisterGenome(referenceTwo.getId());
    }

    @Test
    public void getSomeResultsForSpecifiedPositions() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(idRef)
                .startPosition(1)
                .endPosition(1000)
                .chromosomeId(idChr)
                .motif("AAC")
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertFalse(search.getResult().isEmpty());
    }

    @Test
    public void getSomeResultsForSpecifiedPageSize() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(idRef)
                .startPosition(1)
                .chromosomeId(idChr)
                .motif("AAC")
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(100)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertEquals(search.getResult().size(), (int) att.getPageSize());
    }

    @Test
    public void getResultsFromSeveralChromosomes() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(idRefTwo)
                .chromosomeId(idChrTwo)
                .motif("AAC")
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(10000)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Set<String> chromosomes = search.getResult().stream().map(Motif::getContig)
                .collect(Collectors.toSet());
        Assert.assertTrue(chromosomes.size() > 1);
    }

    @Test
    public void getEmptyPositionForLargeNumberOfResults() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(idRefTwo)
                .startPosition(1)
                .chromosomeId(idChrTwo)
                .motif("AAC")
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(10000)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertNull(search.getPosition());
    }

    @Test
    public void getResultsFromTwoChrWhereRightDataOnlyInFirstChr() throws IOException {
        resource = context.getResource(TEST_WG_PATH);
        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("Test_wg.fa");
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);
        Reference reference = referenceManager.registerGenome(request);
        idRef = reference.getId();
        idChr = reference.getChromosomes().get(0).getId();
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(idRef)
                .startPosition(1)
                .chromosomeId(idChr)
                .motif("CCC")
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(10000)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertNull(search.getPosition());
    }

}
