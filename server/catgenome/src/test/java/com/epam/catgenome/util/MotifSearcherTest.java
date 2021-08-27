package com.epam.catgenome.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class MotifSearcherTest {

    @Test
    public void getPartialResultTest() {
        String testSequence = "cgCGattcGaaGGG";
        String testRegex = "cg";
        Assert.assertEquals(6, MotifSearcher.search(testSequence, testRegex, "").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPartialResultThrowsExceptionOnInvalidSequence() {
        String testSequence = "zxcontig";
        String testRegex = "con";
        MotifSearcher.search(testSequence, testRegex,"");
    }

    @Test
    public void convertIupacToRegexTest(){
        String testRegex = "atcgrYmKsWhBvDn[ac]+";
        String expectedResult = "atcg[rga][ytc][mac][kgt][sgc][wat][hact][bgtc][vgca][dgat].[ac]+";
        Assert.assertEquals(expectedResult, MotifSearcher.convertIupacToRegex(testRegex));
    }
}
