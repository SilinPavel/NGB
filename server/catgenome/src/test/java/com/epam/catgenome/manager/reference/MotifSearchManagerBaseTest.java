package com.epam.catgenome.manager.reference;

import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MotifSearchManagerBaseTest {

    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private static final long[] testChromosomeID = {1001, 1002, 1003, 1004, 1005};
    private static final String[] testChromosomeName = {"1001", "1002", "1003", "1004", "1005"};
    private static final String TEST_CHROMOSOME_SEQUENCE = "actactacttcgtcttgaac";
    private static final long TEST_REFERENCE_ID = 1000;
    private static final int TEST_CHROMOSOME_SIZE = 20;

    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    @Mock
    private ReferenceManager mockedReferenceManager;

    @Mock
    private ReferenceGenomeManager mockedReferenceGenomeManager;

    @Autowired
    @InjectMocks
    private MotifSearchManager motifSearchManager;

//    @Before
//    public void setup() throws IOException {
//        MockitoAnnotations.initMocks(this);
//        final Resource resource = context.getResource("classpath:templates");
//        File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF_NAME);
//
//        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
//        request.setPath(fastaFile.getPath());
//        testReference = referenceManager.registerGenome(request);
//        List<Chromosome> chromosomeList = testReference.getChromosomes();
//        for (Chromosome chromosome : chromosomeList) {
//            if (chromosome.getName().equals(chromosomeName)) {
//                testChromosome = chromosome;
//                break;
//            }
//        }
//    }

    @Test
    public void searchRegionMotifsReference() throws IOException {

        final int startPosition = 0;
        final int endPosition = TEST_CHROMOSOME_SIZE;
        final String refSequence = TEST_CHROMOSOME_SEQUENCE;
        final String expectedSequence = "tact";

        Mockito.when(mockedReferenceManager.getSequenceByteArray(
                        Mockito.eq(startPosition), Mockito.eq(endPosition),
                        Mockito.eq(TEST_REFERENCE_ID), Mockito.anyString()))
                .thenReturn(refSequence.getBytes(StandardCharsets.UTF_8));

        final Chromosome testChromosome0 = new Chromosome(testChromosomeName[0],TEST_CHROMOSOME_SIZE);
        testChromosome0.setId(testChromosomeID[0]);
        final Chromosome testChromosome1 = new Chromosome(testChromosomeName[1],TEST_CHROMOSOME_SIZE);
        testChromosome1.setId(testChromosomeID[1]);
        Mockito.when(mockedReferenceGenomeManager.loadChromosome(testChromosomeID[0]))
                .thenReturn(testChromosome0);
        Mockito.when(mockedReferenceGenomeManager.loadChromosome(testChromosomeID[1]))
                .thenReturn(testChromosome1);

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(TEST_REFERENCE_ID)
                .chromosomeId(testChromosome0.getId())
                .startPosition(startPosition)
                .endPosition(endPosition)
                .pageSize(3)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(expectedSequence)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedSequence, search.getResult().get(0).getSequence());

    }

    @Test
    public void searchChromosomeMotifsReference() throws IOException {

        final int startPosition = 0;
        final int endPosition = TEST_CHROMOSOME_SIZE - 1;
        final String refSequence = TEST_CHROMOSOME_SEQUENCE;
        final String expectedSequence = "tact";

        ReflectionTestUtils.setField(motifSearchManager, "bufferSize", 100);

        Mockito.when(mockedReferenceManager.getSequenceByteArray(
                        Mockito.eq(startPosition), Mockito.eq(endPosition),
                        Mockito.eq(TEST_REFERENCE_ID), Mockito.anyString()))
                .thenReturn(refSequence.getBytes(StandardCharsets.UTF_8));

        final Chromosome testChromosome0 = new Chromosome(testChromosomeName[0],TEST_CHROMOSOME_SIZE);
        testChromosome0.setId(testChromosomeID[0]);
        final Chromosome testChromosome1 = new Chromosome(testChromosomeName[1],TEST_CHROMOSOME_SIZE);
        testChromosome1.setId(testChromosomeID[1]);

        Mockito.when(mockedReferenceGenomeManager.loadChromosome(testChromosomeID[0]))
                .thenReturn(testChromosome0);
        Mockito.when(mockedReferenceGenomeManager.loadChromosome(testChromosomeID[1]))
                .thenReturn(testChromosome1);

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(TEST_REFERENCE_ID)
                .chromosomeId(testChromosome0.getId())
                .startPosition(startPosition)
                .endPosition(endPosition)
                .pageSize(3)
                .includeSequence(true)
                .searchType(MotifSearchType.CHROMOSOME)
                .motif(expectedSequence)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedSequence, search.getResult().get(0).getSequence());

    }
}
