package com.manoj.simcheck.model;

public class MatchedSegment {
    private int doc1Start;
    private int doc1End;
    private int doc2Start;
    private int doc2End;

    public MatchedSegment() {}

    public MatchedSegment(int doc1Start, int doc1End, int doc2Start, int doc2End) {
        this.doc1Start = doc1Start;
        this.doc1End = doc1End;
        this.doc2Start = doc2Start;
        this.doc2End = doc2End;
    }

    public int getDoc1Start() { return doc1Start; }
    public void setDoc1Start(int doc1Start) { this.doc1Start = doc1Start; }
    public int getDoc1End() { return doc1End; }
    public void setDoc1End(int doc1End) { this.doc1End = doc1End; }
    public int getDoc2Start() { return doc2Start; }
    public void setDoc2Start(int doc2Start) { this.doc2Start = doc2Start; }
    public int getDoc2End() { return doc2End; }
    public void setDoc2End(int doc2End) { this.doc2End = doc2End; }
}
