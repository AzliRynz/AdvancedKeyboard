package com.azlirynz.advancedkeyboard.emoji;

import android.content.Context;
import android.util.Log;

import com.azlirynz.advancedkeyboard.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EmojiManager {
    private static final String TAG = "EmojiManager";
    private final Context context;
    private final List<EmojiCategory> categories = new ArrayList<>();
    private final Gson gson = new Gson();

    public EmojiManager(Context context) {
        this.context = context;
    }

    public void load() {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.emoji);
            Type type = new TypeToken<List<EmojiCategory>>(){}.getType();
            List<EmojiCategory> loadedCategories = gson.fromJson(new InputStreamReader(is), type);
            
            categories.clear();
            if (loadedCategories != null) {
                categories.addAll(loadedCategories);
            }
            Log.d(TAG, "Loaded " + categories.size() + " emoji categories");
        } catch (Exception e) {
            Log.e(TAG, "Error loading emoji", e);
        }
    }

    public List<EmojiCategory> getCategories() {
        return new ArrayList<>(categories);
    }

    public List<String> getEmojisForCategory(int categoryIndex) {
        if (categoryIndex >= 0 && categoryIndex < categories.size()) {
            EmojiCategory category = categories.get(categoryIndex);
            if (category != null && category.getEmojis() != null) {
                return new ArrayList<>(category.getEmojis());
            }
        }
        return new ArrayList<>();
    }

    public List<List<String>> getEmojiList() {
        List<List<String>> emojiList = new ArrayList<>();
        for (EmojiCategory category : categories) {
            if (category != null && category.getEmojis() != null) {
                emojiList.add(new ArrayList<>(category.getEmojis()));
            }
        }
        return emojiList;
    }

    public static class EmojiCategory {
        private String name;
        private List<String> emojis;

        public String getName() {
            return name;
        }

        public List<String> getEmojis() {
            return emojis;
        }
    }
}
