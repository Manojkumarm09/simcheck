package com.manoj.simcheck.model;

import java.util.List;

public class CompareResponse {
    private double similarityScore;
    private double doc1Coverage;
    private double doc2Coverage;
    private int totalKgramsDoc1;
    private int totalKgramsDoc2;
    private int matchedKgrams;
    private List<MatchedSegment> matchedSegments;
    private List<String> doc1Tokens;
    private List<String> doc2Tokens;

    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
    public double getDoc1Coverage() { return doc1Coverage; }
    public void setDoc1Coverage(double doc1Coverage) { this.doc1Coverage = doc1Coverage; }
    public double getDoc2Coverage() { return doc2Coverage; }
    public void setDoc2Coverage(double doc2Coverage) { this.doc2Coverage = doc2Coverage; }
    public int getTotalKgramsDoc1() { return totalKgramsDoc1; }
    public void setTotalKgramsDoc1(int totalKgramsDoc1) { this.totalKgramsDoc1 = totalKgramsDoc1; }
    public int getTotalKgramsDoc2() { return totalKgramsDoc2; }
    public void setTotalKgramsDoc2(int totalKgramsDoc2) { this.totalKgramsDoc2 = totalKgramsDoc2; }
    public int getMatchedKgrams() { return matchedKgrams; }
    public void setMatchedKgrams(int matchedKgrams) { this.matchedKgrams = matchedKgrams; }
    public List<MatchedSegment> getMatchedSegments() { return matchedSegments; }
    public void setMatchedSegments(List<MatchedSegment> matchedSegments) { this.matchedSegments = matchedSegments; }
    public List<String> getDoc1Tokens() { return doc1Tokens; }
    public void setDoc1Tokens(List<String> doc1Tokens) { this.doc1Tokens = doc1Tokens; }
    public List<String> getDoc2Tokens() { return doc2Tokens; }
    public void setDoc2Tokens(List<String> doc2Tokens) { this.doc2Tokens = doc2Tokens; }
}
