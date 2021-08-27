package com.epam.catgenome.util;

import org.junit.Assert;
import org.junit.Test;

public class MotifSearcherTest {

    @Test
    public void getPartialResultTest() {
        String testSequence = "cgCGattcGaaGGG";
        String testRegex = "cg";
        Assert.assertEquals(6, MotifSearcher.getPartialResult(testSequence,testRegex,"").size());
    }
}
