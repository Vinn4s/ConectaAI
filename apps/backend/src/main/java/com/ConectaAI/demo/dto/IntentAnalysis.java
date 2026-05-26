package com.ConectaAI.demo.dto;

public class IntentAnalysis {

    private String intent;
    private double confidence;

    public IntentAnalysis() {
    }

    public IntentAnalysis(String intent, double confidence) {
        this.intent = intent;
        this.confidence = confidence;
    }

    public String getIntent() {
        return intent;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}