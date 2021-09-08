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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MotifSearchManagerTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private MotifSearchManager motifSearchManager;

    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private Resource resource;
    private String chromosomeName = "X";
    private Reference testReference;
    private Chromosome testChromosome;

    @Before
    public void setup() throws IOException {
        resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF_NAME);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setPath(fastaFile.getPath());
        testReference = referenceManager.registerGenome(request);
        List<Chromosome> chromosomeList = testReference.getChromosomes();
        for (Chromosome chromosome : chromosomeList) {
            if (chromosome.getName().equals(chromosomeName)) {
                testChromosome = chromosome;
                break;
            }
        }
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

}
