package com.azlirynz.advancedkeyboard;

import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.text.TextUtils;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.azlirynz.advancedkeyboard.databinding.KeyboardLayoutBinding;
import com.azlirynz.advancedkeyboard.R;
import com.azlirynz.advancedkeyboard.dictionary.Dictionary;
import com.azlirynz.advancedkeyboard.dictionary.WordComposer;
import com.azlirynz.advancedkeyboard.emoji.EmojiAdapter;
import com.azlirynz.advancedkeyboard.emoji.EmojiManager;
import com.azlirynz.advancedkeyboard.suggestions.SuggestionAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.ArrayList;

public class AdvancedKeyboardService extends InputMethodService 
    implements KeyboardView.OnKeyboardActionListener,
    SuggestionAdapter.OnSuggestionClickListener,
    EmojiAdapter.OnEmojiClickListener {

    private static final String TAG = "AdvancedKeyboard";
    private KeyboardLayoutBinding binding;
    private View emojiView;
    private Keyboard qwertyKeyboard;
    private Keyboard symbolsKeyboard;
    private Keyboard currentKeyboard;
    private boolean capsLock = false;
    private boolean isEmojiKeyboard = false;
    private boolean isPredictionEnabled = true;
    
    private final WordComposer wordComposer = new WordComposer();
    private Dictionary dictionary;
    private EmojiManager emojiManager;
    private final List<String> suggestions = new ArrayList<>();
    private SuggestionAdapter suggestionAdapter;
    
    private static final int KEYCODE_EMOJI = -100;
    private static final int KEYCODE_LANGUAGE_SWITCH = -101;

    @Override
    public void onCreate() {
        super.onCreate();
        dictionary = new Dictionary(this);
        emojiManager = new EmojiManager(this);
        
        new Thread(() -> {
            dictionary.load();
            emojiManager.load();
            runOnUiThread(() -> {
                if (binding != null) {
                    setupSuggestions();
                }
            });
        }).start();
    }

    @Override
    public View onCreateInputView() {
        binding = KeyboardLayoutBinding.inflate(getLayoutInflater());
        setupMainKeyboard();
        setupSuggestions();
        return binding.getRoot();
    }

    private void setupMainKeyboard() {
        qwertyKeyboard = new Keyboard(this, R.xml.qwerty);
        symbolsKeyboard = new Keyboard(this, R.xml.number_symbols);
        currentKeyboard = qwertyKeyboard;
        
        // Find a suitable key to use as template
        Keyboard.Key templateKey = null;
        for (Keyboard.Key key : qwertyKeyboard.getKeys()) {
            if (key.label != null && key.label.length() == 1) {
                templateKey = key;
                break;
            }
        }
        
        if (templateKey != null) {
            Keyboard.Key emojiKey = new Keyboard.Key(templateKey);
            emojiKey.codes = new int[]{KEYCODE_EMOJI};
            emojiKey.label = "😀";
            
            try {
                Drawable icon = getResources().getDrawable(R.drawable.ic_emoji);
                if (icon != null) {
                    emojiKey.icon = icon;
                    emojiKey.label = null;
                }
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Emoji icon not found");
            }
            
            qwertyKeyboard.getKeys().add(emojiKey);
        }

        binding.keyboardView.setKeyboard(currentKeyboard);
        binding.keyboardView.setOnKeyboardActionListener(this);
        binding.keyboardView.setPreviewEnabled(false); // Better UX for most keyboards
    }

    private void setupEmojiKeyboard() {
        if (emojiView == null) {
            emojiView = getLayoutInflater().inflate(R.layout.emoji_keyboard, null);
            
            ViewPager emojiViewPager = emojiView.findViewById(R.id.emojiViewPager);
            TabLayout emojiCategories = emojiView.findViewById(R.id.emojiCategories);
            
            EmojiAdapter emojiAdapter = new EmojiAdapter(emojiManager.getEmojiList(), this);
            emojiViewPager.setAdapter(emojiAdapter);
            emojiCategories.setupWithViewPager(emojiViewPager);
            
            // Set custom tab views
            for (int i = 0; i < emojiCategories.getTabCount(); i++) {
                TabLayout.Tab tab = emojiCategories.getTabAt(i);
                if (tab != null) {
                    tab.setCustomView(emojiAdapter.getTabView(i));
                }
            }
        }
    }

    private void setupSuggestions() {
        suggestionAdapter = new SuggestionAdapter(suggestions, this);
        binding.suggestionsRecycler.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.suggestionsRecycler.setAdapter(suggestionAdapter);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        switch(primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                handleBackspace(ic);
                break;
                
            case Keyboard.KEYCODE_SHIFT:
                handleShift();
                break;
                
            case Keyboard.KEYCODE_MODE_CHANGE:
                toggleKeyboardMode();
                break;
                
            case KEYCODE_EMOJI:
                toggleEmojiKeyboard();
                break;
                
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                break;
                
            default:
                handleCharacterInput(ic, primaryCode, keyCodes);
        }
    }

    private void handleBackspace(InputConnection ic) {
        CharSequence before = ic.getTextBeforeCursor(1, 0);
        if (!TextUtils.isEmpty(before)) {
            ic.deleteSurroundingText(1, 0);
            if (isPredictionEnabled) {
                wordComposer.deleteLast();
                updateSuggestions();
            }
        }
    }

    private void handleShift() {
        boolean isShifted = binding.keyboardView.isShifted();
        if (isShifted && capsLock) {
            binding.keyboardView.setShifted(false);
            capsLock = false;
        } else if (isShifted) {
            capsLock = true;
        } else {
            binding.keyboardView.setShifted(true);
        }
        binding.keyboardView.invalidateAllKeys();
    }

    private void toggleKeyboardMode() {
        currentKeyboard = (currentKeyboard == qwertyKeyboard) ? symbolsKeyboard : qwertyKeyboard;
        binding.keyboardView.setKeyboard(currentKeyboard);
        binding.keyboardView.invalidateAllKeys();
    }

    private void toggleEmojiKeyboard() {
        isEmojiKeyboard = !isEmojiKeyboard;
        if (isEmojiKeyboard) {
            setupEmojiKeyboard();
            setInputView(emojiView);
        } else {
            setInputView(binding.getRoot());
        }
    }

    private void handleCharacterInput(InputConnection ic, int primaryCode, int[] keyCodes) {
        char code = (char) primaryCode;
        if (Character.isLetter(code)) {
            code = binding.keyboardView.isShifted() ? Character.toUpperCase(code) : Character.toLowerCase(code);
            if (!capsLock) {
                binding.keyboardView.setShifted(false);
            }
        }
        
        ic.commitText(String.valueOf(code), 1);
        
        if (isPredictionEnabled && Character.isLetter(code)) {
            wordComposer.add(code, keyCodes);
            updateSuggestions();
        } else {
            clearSuggestions();
        }
    }

    private void updateSuggestions() {
        suggestions.clear();
        List<String> newSuggestions = dictionary.getSuggestions(wordComposer);
        if (newSuggestions != null) {
            suggestions.addAll(newSuggestions);
        }
        suggestionAdapter.notifyDataSetChanged();
    }

    private void clearSuggestions() {
        suggestions.clear();
        suggestionAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSuggestionClick(String word) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.deleteSurroundingText(wordComposer.size(), 0);
            ic.commitText(word, 1);
            wordComposer.reset();
            clearSuggestions();
        }
    }

    @Override
    public void onEmojiClick(String emoji) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(emoji, 1);
        }
    }

    // Required keyboard action listener methods
    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
