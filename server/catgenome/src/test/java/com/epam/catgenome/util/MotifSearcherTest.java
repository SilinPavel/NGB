package com.epam.catgenome.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class MotifSearcherTest {

    @Test
    public void searchTest() {
        byte[] testSequence = "cgCGattcGaaGGG".getBytes(StandardCharsets.UTF_8);
        String testRegex = "cg";
        Assert.assertEquals(6, MotifSearcher.search(testSequence, testRegex, "").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchThrowsExceptionOnInvalidSequence() {
        byte[] testSequence = "zxcontig".getBytes(StandardCharsets.UTF_8);
        String testRegex = "con";
        MotifSearcher.search(testSequence, testRegex, "");
    }

    @Test
    public void convertIupacToRegexTest(){
        String testRegex = "atcgrYmKsWhBvDn[ac]+";
        String expectedResult = "atcg[rga][ytc][mac][kgt][sgc][wat][hact][bgtc][vgca][dgat].[ac]+";
        Assert.assertEquals(expectedResult, MotifSearcher.convertIupacToRegex(testRegex));
    }

    @Test
    public void searchInLargeBufferTest() throws IOException {

        final int expectedSize = 5592;
        final int bufferSize = 50_000_000;

        byte[] buf = new byte[bufferSize];
        getClass().getResourceAsStream("/templates/A3.fa").read(buf);
        String testSequence = new String(buf);
        final Pattern pattern = Pattern.compile("[^ATCGNatcgn]");
        testSequence = pattern.matcher(testSequence).replaceAll("n");
        buf = testSequence.getBytes(StandardCharsets.UTF_8);
        String testRegex = "ta";
        Assert.assertEquals(expectedSize, MotifSearcher.search(buf, testRegex, "").size());
    }
}
