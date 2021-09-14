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

package com.epam.catgenome.util;

import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.util.motif.IupacRegexConverter;
import com.epam.catgenome.util.motif.MotifSearcher;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class MotifSearcherTest {


    public static final String TEST_MOTIF = "cgCGcattgcGcaaGGG";

    @Test
    public void searchTest() {
        byte[] testSequence = TEST_MOTIF.getBytes(StandardCharsets.UTF_8);
        String testRegex = "ca";
        Assert.assertEquals(3, MotifSearcher.search(testSequence, testRegex, "", 0, true).size());
    }

    @Test
    public void searchBoundsTest() {
        byte[] testSequence = TEST_MOTIF.getBytes(StandardCharsets.UTF_8);
        String testRegex = "ca";
        final Motif firstSearchResult = MotifSearcher.search(testSequence, testRegex, "", 0, true).get(0);
        Assert.assertEquals(4, firstSearchResult.getStart());
        Assert.assertEquals(5, firstSearchResult.getEnd());
    }

    @Test
    public void searchSequenceTest() {
        byte[] testSequence = TEST_MOTIF.getBytes(StandardCharsets.UTF_8);
        String testRegex = "ca";
        final Motif firstSearchResult = MotifSearcher.search(testSequence, testRegex, "", 0, true).get(0);
        Assert.assertEquals(testRegex, firstSearchResult.getSequence());
    }

    @Test
    public void searchInPositiveStrandTest() {
        byte[] testSequence = TEST_MOTIF.getBytes(StandardCharsets.UTF_8);
        String testRegex = "ca";
        Assert.assertEquals(2, MotifSearcher.search(testSequence, testRegex,
                StrandSerializable.POSITIVE, "", 0, true).size());
    }

    @Test
    public void searchInNegativeStrandTest() {
        byte[] testSequence = TEST_MOTIF.getBytes(StandardCharsets.UTF_8);
        String testRegex = "ca";
        Assert.assertEquals(1, MotifSearcher.search(testSequence, testRegex,
                StrandSerializable.NEGATIVE, "", 0, true).size());
    }

    @Test(expected = IllegalStateException.class)
    public void searchThrowsExceptionOnInvalidSequence() {
        byte[] testSequence = "zxcontig".getBytes(StandardCharsets.UTF_8);
        String testRegex = "con";
        MotifSearcher.search(testSequence, testRegex, "", 0, true);
    }

    @Test
    public void convertIupacToRegexTest(){
        String testRegex = "atcgrYmKsWhBvDn[ac]+";
        String expectedResult = "atcg[rga][ytc][mac][kgt][sgc][wat][hact][bgtc][vgca][dgat].[ac]+";
        Assert.assertEquals(expectedResult, IupacRegexConverter.convertIupacToRegex(testRegex));
    }

    @Test
    public void convertIupactoComplementReversedRegexTest(){
        String testRegex1 = "a[^a[tcg]rY]mK{23}sn[ac]+";
        String expectedResult1 = "[gt]+.[cgs][acm]{23}[gtk][^[gar][tcy][cga]t]t";
        Assert.assertEquals(expectedResult1, IupacRegexConverter.convertIupacToComplementReversedRegex(testRegex1));

        String testRegex2 = "a+?c[gt]*?yr{2,}a{7,}";
        String expectedResult2 = "t{7,}[tcy]{2,}[gar][ac]*?gt+?";
        Assert.assertEquals(expectedResult2, IupacRegexConverter.convertIupacToComplementReversedRegex(testRegex2));

        String testRegex3 = "(ac)|(tc)|[tcc]{12}";
        String expectedResult3 = "[gga]{12}|(ga)|(gt)";
        Assert.assertEquals(expectedResult3, IupacRegexConverter.convertIupacToComplementReversedRegex(testRegex3));

        String testRegex4 = "((ac)|(tc))|([tcc]{12,23})";
        String expectedResult4 = "([gga]{12,23})|((ga)|(gt))";
        Assert.assertEquals(expectedResult4, IupacRegexConverter.convertIupacToComplementReversedRegex(testRegex4));
    }

    @Test
    public void searchInLargeBufferTest() throws IOException {

        final int expectedSize = 57534;
        final int bufferSize = 50_000_000;

        byte[] buf = new byte[bufferSize];
        final InputStream resourceAsStream = getClass().getResourceAsStream("/templates/dm606.X.fa");
        resourceAsStream.read(buf);
        resourceAsStream.close();
        String testSequence = new String(buf);
        final Pattern pattern = Pattern.compile("[^ATCGNatcgn]");
        testSequence = pattern.matcher(testSequence).replaceAll("n");
        buf = testSequence.getBytes(StandardCharsets.UTF_8);
        String testRegex = "tacyrw";
        Assert.assertEquals(expectedSize, MotifSearcher.search(buf, testRegex, "", 0, true).size());
    }

    @Test
    public void searchInLargeBufferInPositiveAndNegativeStrandTest() throws IOException {

        final int expectedSize = 57534;
        final int bufferSize = 50_000_000;

        byte[] buf = new byte[bufferSize];
        final InputStream resourceAsStream = getClass().getResourceAsStream("/templates/dm606.X.fa");
        resourceAsStream.read(buf);
        resourceAsStream.close();
        String testSequence = new String(buf);
        final Pattern pattern = Pattern.compile("[^ATCGNatcgn]");
        testSequence = pattern.matcher(testSequence).replaceAll("n");
        buf = testSequence.getBytes(StandardCharsets.UTF_8);
        String testRegex = "tacyrw";
        final int sumResult = MotifSearcher.search(buf, testRegex, StrandSerializable.POSITIVE, "", 0, true).size() +
                MotifSearcher.search(buf, testRegex, StrandSerializable.NEGATIVE, "", 0, true).size();
        Assert.assertEquals(expectedSize, sumResult);
    }
}
