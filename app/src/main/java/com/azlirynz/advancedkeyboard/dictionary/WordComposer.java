package com.azlirynz.advancedkeyboard.dictionary;

import java.util.ArrayList;
import java.util.List;

public class WordComposer {
    private final List<Character> chars = new ArrayList<>();
    private final List<int[]> keyCodes = new ArrayList<>();

    public void add(char c, int[] codes) {
        chars.add(c);
        keyCodes.add(codes);
    }

    public void deleteLast() {
        if (!chars.isEmpty()) {
            chars.remove(chars.size() - 1);
            keyCodes.remove(keyCodes.size() - 1);
        }
    }

    public void reset() {
        chars.clear();
        keyCodes.clear();
    }

    public int size() {
        return chars.size();
    }

    public CharSequence getTypedWord() {
        StringBuilder sb = new StringBuilder();
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    public int[] getCodesAt(int index) {
        return keyCodes.get(index);
    }
}