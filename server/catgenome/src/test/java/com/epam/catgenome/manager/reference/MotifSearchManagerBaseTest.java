package com.epam.catgenome.manager.reference;

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import org.hamcrest.text.IsEqualIgnoringCase;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MotifSearchManagerBaseTest {

    private static final String TEST_REF_NAME = "//dm606.X.fa";

    @Autowired
    ApplicationContext context;

    @Mock
    private ReferenceManager mockedReferenceManager;

    @Mock
    private ReferenceGenomeManager mockedReferenceGenomeManager;

    @InjectMocks
    private MotifSearchManager mockedMotifSearchManager;

    private Map<Long, String> chromosomeTestNames;
    private Map<Long, byte[]> testChrSequences;
    private final long refTestID = 123;


    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        final Resource resource = context.getResource("classpath:templates");
        chromosomeTestNames = new HashMap<>();
        testChrSequences = new HashMap<>();
        testChrSequences.put(0L, readFirstChromosome(resource.getFile().getAbsolutePath(), TEST_REF_NAME));
        testChrSequences.put(1L, reverse(testChrSequences.get(0L)));
        chromosomeTestNames.put(0L,"A");
        chromosomeTestNames.put(1L,"B");
        mockFetchChromosomes();
    }

    @Test
    public void searchRegionMotifsReference() throws IOException {

        final int testStart = 9000;
        final int testEnd = 10000;
        final int testMotifStart = 9100;
        final int testMotifEnd = 9200;
        final long chromosomeID = 0L;
        final String testMotif =
                new String(testChrSequences.get(chromosomeID)).substring(testMotifStart, testMotifEnd + 1);

        mockSequenceSource(testStart, testEnd, chromosomeID);

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(refTestID)
                .chromosomeId(chromosomeID)
                .startPosition(testStart)
                .endPosition(testEnd)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = mockedMotifSearchManager.search(testRequest);
        Assert.assertThat(testMotif, IsEqualIgnoringCase.equalToIgnoringCase(search.getResult().get(0).getSequence()));
        Assert.assertEquals(testMotifStart, search.getResult().get(0).getStart());
        Assert.assertEquals(testMotifEnd, search.getResult().get(0).getEnd());
    }

    @Test
    public void searchChromosomeMotifsReference() throws IOException {

        final int testStart = 1;
        final int testEnd = testChrSequences.get(0L).length;
        final long chromosomeID = 0L;

        ReflectionTestUtils.setField(mockedMotifSearchManager, "bufferSize", 50);

        mockSequenceSource(testStart, testEnd, chromosomeID);

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(refTestID)
                .chromosomeId(chromosomeID)
                .startPosition(testStart)
                .endPosition(testEnd)
                .includeSequence(true)
                .searchType(MotifSearchType.CHROMOSOME)
                .motif("acgtrytatcgt")
                .pageSize(1000)
                .slidingWindow(15)
                .build();
//        final MotifSearchResult searchWithSmallBuffer = mockedMotifSearchManager.search(testRequest);
//        final MotifSearchResult searchWithNormalBuffer = motifSearchManager.search(testRequest);
//        Assert.assertEquals( searchWithNormalBuffer.getResult().size(), searchWithSmallBuffer.getResult().size());

    }

    private static byte[] reverse(final byte[] seq) {
        final byte[] copiedSeq = seq.clone();
        for (int i = 0, j = copiedSeq.length - 1; i < copiedSeq.length / 2; i++, j--) {
            byte swapVal = copiedSeq[i];
            copiedSeq[i] = copiedSeq[j];
            copiedSeq[j] = swapVal;
        }
        return copiedSeq;
    }

    private void mockSequenceSource(final int start, final int end, final long chrID) throws IOException {
        Mockito.when(mockedReferenceManager.getSequenceByteArray(
                        Mockito.eq(start), Mockito.eq(end),
                        (Reference) Mockito.anyObject(), Mockito.eq(chromosomeTestNames.get(chrID))))
                .thenReturn(Arrays.copyOfRange(testChrSequences.get(chrID), start, end + 1));
    }

    private void mockFetchChromosomes() throws IOException {
        final List<Chromosome> testChromosomes = Arrays.asList(
                new Chromosome(chromosomeTestNames.get(0L), testChrSequences.get(0L).length),
                new Chromosome(chromosomeTestNames.get(1L), testChrSequences.get(1L).length));
        IntStream.rangeClosed(0,1).forEachOrdered(i -> testChromosomes.get(i).setId((long)i));

        Mockito.when(mockedReferenceGenomeManager.loadChromosomes(refTestID))
                .thenReturn(testChromosomes);
        Mockito.when(mockedReferenceGenomeManager.getOnlyReference(refTestID))
                .thenReturn(new Reference());
    }

    private byte[] readFirstChromosome(final String pathToDirectory, final String... pathToFastaFile)
            throws IOException {
        return Files.readAllLines(Paths.get(pathToDirectory, pathToFastaFile)).stream()
                .skip(1)
                .collect(Collectors.joining("")).getBytes(StandardCharsets.UTF_8);
    }
}
