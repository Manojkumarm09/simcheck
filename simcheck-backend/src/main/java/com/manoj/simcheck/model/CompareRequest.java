package com.manoj.simcheck.model;

public class CompareRequest {
    private String doc1;
    private String doc2;
    private String mode = "TEXT"; // TEXT or CODE
    private int kgramSize = 5;

    public String getDoc1() { return doc1; }
    public void setDoc1(String doc1) { this.doc1 = doc1; }
    public String getDoc2() { return doc2; }
    public void setDoc2(String doc2) { this.doc2 = doc2; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public int getKgramSize() { return kgramSize; }
    public void setKgramSize(int kgramSize) { this.kgramSize = kgramSize; }
}
