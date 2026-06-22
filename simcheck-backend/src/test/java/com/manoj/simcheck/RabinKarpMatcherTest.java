package com.manoj.simcheck;

import com.manoj.simcheck.service.RabinKarpMatcher;
import com.manoj.simcheck.service.TokenizerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RabinKarpMatcherTest {

    private final RabinKarpMatcher matcher = new RabinKarpMatcher();
    private final TokenizerService tokenizer = new TokenizerService();

    @Test
    void identicalTextsShouldMatch() {
        List<String> tokens1 = tokenizer.tokenize("the quick brown fox jumps over the lazy dog", "TEXT");
        List<String> tokens2 = tokenizer.tokenize("the quick brown fox jumps over the lazy dog", "TEXT");

        RabinKarpMatcher.KGramIndex i1 = matcher.buildIndex(tokens1, 3);
        RabinKarpMatcher.KGramIndex i2 = matcher.buildIndex(tokens2, 3);

        assertFalse(matcher.findMatches(tokens1, i1, tokens2, i2, 3).isEmpty());
    }

    @Test
    void completelyDifferentTextsShouldNotMatch() {
        List<String> tokens1 = tokenizer.tokenize("alpha beta gamma delta epsilon", "TEXT");
        List<String> tokens2 = tokenizer.tokenize("zebra mountain ocean violin guitar", "TEXT");

        RabinKarpMatcher.KGramIndex i1 = matcher.buildIndex(tokens1, 3);
        RabinKarpMatcher.KGramIndex i2 = matcher.buildIndex(tokens2, 3);

        assertTrue(matcher.findMatches(tokens1, i1, tokens2, i2, 3).isEmpty());
    }

    @Test
    void partialOverlapShouldFindSomeMatches() {
        List<String> tokens1 = tokenizer.tokenize("public int add(int a, int b) { return a + b; }", "CODE");
        List<String> tokens2 = tokenizer.tokenize("public int sum(int x, int y) { return a + b; }", "CODE");

        RabinKarpMatcher.KGramIndex i1 = matcher.buildIndex(tokens1, 3);
        RabinKarpMatcher.KGramIndex i2 = matcher.buildIndex(tokens2, 3);

        assertFalse(matcher.findMatches(tokens1, i1, tokens2, i2, 3).isEmpty());
    }
}
