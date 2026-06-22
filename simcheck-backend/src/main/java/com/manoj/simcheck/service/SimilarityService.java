package com.manoj.simcheck.service;

import com.manoj.simcheck.model.*;
import com.manoj.simcheck.util.IntervalMerger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimilarityService {

    @Autowired private TokenizerService tokenizerService;
    @Autowired private RabinKarpMatcher matcher;
    @Autowired private IntervalMerger merger;

    public CompareResponse compare(CompareRequest request) {
        // Original tokens are kept for display/highlighting (the user's real text/code).
        List<String> originalTokens1 = tokenizerService.tokenize(request.getDoc1(), request.getMode());
        List<String> originalTokens2 = tokenizerService.tokenize(request.getDoc2(), request.getMode());

        // Match tokens are what the algorithm actually compares — for code mode
        // these have identifiers normalized so renamed variables still match.
        // Index-for-index aligned with the original tokens, so positions carry over.
        List<String> tokens1 = tokenizerService.toMatchTokens(originalTokens1, request.getMode());
        List<String> tokens2 = tokenizerService.toMatchTokens(originalTokens2, request.getMode());

        int k = Math.max(1, request.getKgramSize());

        RabinKarpMatcher.KGramIndex index1 = matcher.buildIndex(tokens1, k);
        RabinKarpMatcher.KGramIndex index2 = matcher.buildIndex(tokens2, k);

        List<int[]> rawMatches = matcher.findMatches(tokens1, index1, tokens2, index2, k);

        List<IntervalMerger.Range> doc1Ranges = new ArrayList<>();
        List<IntervalMerger.Range> doc2Ranges = new ArrayList<>();
        for (int[] m : rawMatches) {
            doc1Ranges.add(new IntervalMerger.Range(m[0], m[1]));
            doc2Ranges.add(new IntervalMerger.Range(m[2], m[3]));
        }

        List<IntervalMerger.Range> mergedDoc1 = merger.merge(doc1Ranges);
        List<IntervalMerger.Range> mergedDoc2 = merger.merge(doc2Ranges);

        List<MatchedSegment> segments = new ArrayList<>();
        int maxSize = Math.max(mergedDoc1.size(), mergedDoc2.size());
        for (int i = 0; i < maxSize; i++) {
            int s1 = i < mergedDoc1.size() ? mergedDoc1.get(i).start : -1;
            int e1 = i < mergedDoc1.size() ? mergedDoc1.get(i).end : -1;
            int s2 = i < mergedDoc2.size() ? mergedDoc2.get(i).start : -1;
            int e2 = i < mergedDoc2.size() ? mergedDoc2.get(i).end : -1;
            segments.add(new MatchedSegment(s1, e1, s2, e2));
        }
          int matchedKgramCount = matcher.countSharedKgrams(index1, index2, tokens1, tokens2, k);
        int totalK1 = index1.totalKgrams;
        int totalK2 = index2.totalKgrams;

        double similarity = (totalK1 + totalK2) == 0 ? 0 :
                (200.0 * matchedKgramCount) / (totalK1 + totalK2);
        double coverage1 = originalTokens1.isEmpty() ? 0 : (100.0 * coveredCount(mergedDoc1)) / originalTokens1.size();
double coverage2 = originalTokens2.isEmpty() ? 0 : (100.0 * coveredCount(mergedDoc2)) / originalTokens2.size();

        CompareResponse response = new CompareResponse();
        response.setSimilarityScore(round(similarity));
        response.setDoc1Coverage(round(coverage1));
        response.setDoc2Coverage(round(coverage2));
        response.setTotalKgramsDoc1(totalK1);
        response.setTotalKgramsDoc2(totalK2);
        response.setMatchedKgrams(matchedKgramCount);
        response.setMatchedSegments(segments);
        response.setDoc1Tokens(originalTokens1);
        response.setDoc2Tokens(originalTokens2);
        return response;
    }

   

    private int coveredCount(List<IntervalMerger.Range> merged) {
        int total = 0;
        for (IntervalMerger.Range r : merged) total += (r.end - r.start);
        return total;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}