package com.azlirynz.advancedkeyboard.dictionary;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Dictionary {
    private static final String TAG = "Dictionary";
    private final Context context;
    private final TrieNode root = new TrieNode();
    private final List<String> words = new ArrayList<>();
    private final Gson gson = new Gson();

    public Dictionary(Context context) {
        this.context = context;
    }

    public void load() {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.dictionary);
            Type type = new TypeToken<List<String>>(){}.getType();
            List<String> dictionaryWords = gson.fromJson(new InputStreamReader(is), type);

            for (String word : dictionaryWords) {
                String cleanWord = word.trim().toLowerCase();
                if (!cleanWord.isEmpty()) {
                    insertWord(cleanWord);
                    words.add(cleanWord);
                }
            }
            Log.d(TAG, "Loaded " + words.size() + " words");
        } catch (Exception e) {
            Log.e(TAG, "Error loading dictionary", e);
        }
    }

    private void insertWord(String word) {
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            current = current.getChildren().computeIfAbsent(c, k -> new TrieNode());
        }
        current.setEndOfWord(true);
    }

    public List<String> getSuggestions(WordComposer composer) {
        List<String> suggestions = new ArrayList<>();
        String prefix = composer.getTypedWord().toString().toLowerCase();

        // Exact match first
        if (words.contains(prefix)) {
            suggestions.add(prefix);
        }

        // Then find similar words in trie
        findSuggestionsInTrie(prefix, suggestions);

        // Finally use edit distance if needed
        if (suggestions.size() < 3) {
            findSimilarWords(prefix, suggestions);
        }

        return suggestions.subList(0, Math.min(5, suggestions.size()));
    }

    private void findSuggestionsInTrie(String prefix, List<String> suggestions) {
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            node = node.getChildren().get(c);
            if (node == null) return;
        }
        findAllWords(node, prefix, suggestions);
    }

    private void findAllWords(TrieNode node, String prefix, List<String> words) {
        if (node.isEndOfWord() && !words.contains(prefix)) {
            words.add(prefix);
        }
        for (Map.Entry<Character, TrieNode> entry : node.getChildren().entrySet()) {
            findAllWords(entry.getValue(), prefix + entry.getKey(), words);
        }
    }

    private void findSimilarWords(String word, List<String> suggestions) {
        for (String dictWord : words) {
            if (editDistance(word, dictWord) <= 2 && !suggestions.contains(dictWord)) {
                suggestions.add(dictWord);
                if (suggestions.size() >= 5) break;
            }
        }
    }

    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(
                            dp[i-1][j-1] + (a.charAt(i-1) == b.charAt(j-1) ? 0 : 1),
                            dp[i-1][j] + 1,
                            dp[i][j-1] + 1
                    );
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    private int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }
}