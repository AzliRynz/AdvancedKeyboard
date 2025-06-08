package com.azlirynz.advancedkeyboard.emoji;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.azlirynz.advancedkeyboard.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmojiManager {
    private static final String TAG = "EmojiManager";
    private final Context context;
    private final List<EmojiCategory> categories = new ArrayList<>();
    private final Gson gson = new Gson();

    public EmojiManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void load() {
        try (InputStream is = context.getResources().openRawResource(R.raw.emoji)) {
            Type type = new TypeToken<List<EmojiCategory>>(){}.getType();
            List<EmojiCategory> loadedCategories = gson.fromJson(new InputStreamReader(is), type);
            
            categories.clear();
            if (loadedCategories != null) {
                categories.addAll(loadedCategories);
            }
            Log.d(TAG, "Loaded " + categories.size() + " emoji categories");
        } catch (IOException e) {
            Log.e(TAG, "Error loading emoji", e);
        }
    }

    @NonNull
    public List<EmojiCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    @NonNull
    public List<String> getEmojisForCategory(int categoryIndex) {
        if (categoryIndex >= 0 && categoryIndex < categories.size()) {
            EmojiCategory category = categories.get(categoryIndex);
            if (category != null && category.getEmojis() != null) {
                return Collections.unmodifiableList(category.getEmojis());
            }
        }
        return Collections.emptyList();
    }

    public static final class EmojiCategory {
        private final String name;
        private final List<String> emojis;

        public EmojiCategory(String name, List<String> emojis) {
            this.name = name;
            this.emojis = emojis != null ? new ArrayList<>(emojis) : new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        @NonNull
        public List<String> getEmojis() {
            return Collections.unmodifiableList(emojis);
        }
    }
}
