package com.azlirynz.advancedkeyboard.suggestions;

public class SuggestionItem {
    private final String text;
    private final boolean isPrediction;

    public SuggestionItem(String text, boolean isPrediction) {
        this.text = text;
        this.isPrediction = isPrediction;
    }

    public String getText() {
        return text;
    }

    public boolean isPrediction() {
        return isPrediction;
    }
}