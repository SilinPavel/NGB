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

import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import org.junit.After;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional
public class MotifSearchManagerTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private MotifSearchManager motifSearchManager;

    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private static final String CHROMOSOME_NAME = "X";

    private static final String A3_FA_PATH = "classpath:templates/A3.fa";
    private static final String HP_GENOME_PATH = "classpath:templates/reference/hp.genome.fa";
    private static final String TEST_WG_PATH = "classpath:templates/Test_wg.fa";

    private Resource resource;
    private Reference testReference;
    private Chromosome testChromosome;
    private int motifSearchManagerBufferSize;

    @Before
    public void setup() throws IOException {
        motifSearchManagerBufferSize = (int)ReflectionTestUtils.getField(motifSearchManager, "bufferSize");
        resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF_NAME);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setPath(fastaFile.getPath());
        testReference = referenceManager.registerGenome(request);
        List<Chromosome> chromosomeList = testReference.getChromosomes();
        for (Chromosome chromosome : chromosomeList) {
            if (chromosome.getName().equals(CHROMOSOME_NAME)) {
                testChromosome = chromosome;
                break;
            }
        }
    }

    @After
    public void restoreMotifSearchManagerBufferSize() {
        ReflectionTestUtils.setField(motifSearchManager, "bufferSize", motifSearchManagerBufferSize);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifsBounds() throws IOException {
        final int expectedStartPosition = 10000;
        final int expectedEndPosition = 11000;
        final int shift = 1000;

        final String testMotif = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition - shift)
                .endPosition(expectedEndPosition + shift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedStartPosition, search.getResult().get(0).getStart());
        Assert.assertEquals(expectedEndPosition, search.getResult().get(0).getEnd());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifsSequence() throws IOException {
        final int startPosition = 20000;
        final int endPosition = 21000;
        final int shift = 1000;

        final String expectedSequence = referenceManager.getSequenceString(
                startPosition,
                endPosition,
                testReference.getId(),
                testChromosome.getName());
        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(startPosition - shift)
                .endPosition(endPosition + shift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(expectedSequence)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedSequence, search.getResult().get(0).getSequence());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifsReturnsEmptyValidRespondWhenMatchesNotFound() {

        final int testStart = 1000;
        final int testEnd = 1050;
        final String testMotif = "acgttcgaacgttcga";

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(testStart)
                .endPosition(testEnd)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();

        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(0, search.getResult().size());
        Assert.assertEquals(0, search.getPageSize().longValue());
        Assert.assertEquals(testStart + 1, search.getPosition().longValue());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchChromosomeMotifsBounds() throws IOException {
        final int expectedEndPosition = testChromosome.getSize();
        final int expectedStartPosition = expectedEndPosition - 1000;
        final int shift = 1000;

        final String testMotif = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition - shift)
                .includeSequence(true)
                .searchType(MotifSearchType.CHROMOSOME)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedStartPosition, search.getResult().get(0).getStart());
        Assert.assertEquals(expectedEndPosition, search.getResult().get(0).getEnd());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchChromosomeMotifsSequence() throws IOException {
        final int expectedEndPosition = testChromosome.getSize();
        final int expectedStartPosition = expectedEndPosition - 10;
        final int shift = 10;

        final String testMotif = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition - shift)
                .includeSequence(true)
                .searchType(MotifSearchType.CHROMOSOME)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(testMotif, search.getResult().get(0).getSequence());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchChromosomeMotifsShouldResultWithTheSameResultWhenBufferVaried() {

        final int testStart = 1;
        final int testEnd = 1000000;
        final int pageSize = 10000;
        final int slidingWindow = 15;
        final int expectedResultSize = 160;
        final int initialTestBufferSize = 50;
        final int testBufferSizeStep = 123;
        final int numberOfBufferSteps = 5;
        final String testMotif = "acrywagt";


        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(testStart)
                .endPosition(testEnd)
                .includeSequence(true)
                .searchType(MotifSearchType.CHROMOSOME)
                .motif(testMotif)
                .pageSize(pageSize)
                .slidingWindow(slidingWindow)
                .build();

        final MotifSearchResult searchWithNormalBuffer = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedResultSize, searchWithNormalBuffer.getResult().size());

        IntStream.iterate(initialTestBufferSize, i -> i + testBufferSizeStep).limit(numberOfBufferSteps).forEach(i -> {
            ReflectionTestUtils.setField(motifSearchManager, "bufferSize", i);
            final MotifSearchResult searchWithSmallBuffer = motifSearchManager.search(testRequest);
            Assert.assertEquals(searchWithNormalBuffer.getResult().size(), searchWithSmallBuffer.getResult().size());
        });
    }
    private static final String A3_FA_PATH = "classpath:templates/A3.fa";
    private static final String HP_GENOME_PATH = "classpath:templates/reference/hp.genome.fa";
    private static final String TEST_WG_PATH = "classpath:templates/Test_wg.fa";

    private Resource resource;
    private long idRef;
    private long idChr;
    private long idRefTwo;
    private long idChrTwo;

    @Before
    public void registerFilesForTest() throws IOException {
        resource = context.getResource(A3_FA_PATH);
        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("A3.fa");
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);
        Reference reference = referenceManager.registerGenome(request);
        idRef = reference.getId();
        idChr = reference.getChromosomes().get(0).getId();
        resource = context.getResource(HP_GENOME_PATH);
        request = new ReferenceRegistrationRequest();
        request.setName("hp.genome.fa");
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);
        reference = referenceManager.registerGenome(request);
        idRefTwo = reference.getId();
        idChrTwo = reference.getChromosomes().get(1).getId();
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
    public void checkThatPositionIsNullWhenWeSearchForLargeNumberOfResults() {
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
        Assert.assertFalse(search.getResult().isEmpty());
    }

    @Test
    public void checkThatPositionIsNullWhenWeSearchFullGenomeButDataOnlyInFirstChrTest()
            throws IOException {
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
