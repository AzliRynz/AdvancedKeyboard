package com.azlirynz.advancedkeyboard;

import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.text.TextUtils;
import android.graphics.drawable.Drawable;

import androidx.recyclerview.widget.LinearLayoutManager;
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

    // Binding for keyboard layout
    private KeyboardLayoutBinding binding;
    
    // Emoji keyboard view
    private View emojiView;
    
    // Keyboard layouts
    private Keyboard qwertyKeyboard;
    private Keyboard symbolsKeyboard;
    private Keyboard currentKeyboard;
    
    // Keyboard state flags
    private boolean capsLock = false;
    private boolean isEmojiKeyboard = false;
    private boolean isPredictionEnabled = true;
    
    // Text prediction components
    private final WordComposer wordComposer = new WordComposer();
    private Dictionary dictionary;
    private EmojiManager emojiManager;
    
    // Suggestions list
    private final List<String> suggestions = new ArrayList<>();
    private SuggestionAdapter suggestionAdapter;
    
    // Custom key codes
    private static final int KEYCODE_EMOJI = -100;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize dictionary and emoji manager
        dictionary = new Dictionary(this);
        emojiManager = new EmojiManager(this);
        
        // Load resources in background thread
        new Thread(() -> {
            dictionary.load();
            emojiManager.load();
        }).start();
    }

    @Override
    public View onCreateInputView() {
        binding = KeyboardLayoutBinding.inflate(getLayoutInflater());
        
        // Set up main keyboard and suggestions
        setupMainKeyboard();
        setupSuggestions();
        
        return binding.getRoot();
    }

    private void setupMainKeyboard() {
        // Load keyboard layouts from XML
        qwertyKeyboard = new Keyboard(this, R.xml.qwerty);
        symbolsKeyboard = new Keyboard(this, R.xml.number_symbols);
        currentKeyboard = qwertyKeyboard;
        
        // Add custom emoji key
        Keyboard.Key emojiKey = new Keyboard.Key(qwertyKeyboard.getKeys().get(0));
        emojiKey.codes = new int[] {KEYCODE_EMOJI};
        emojiKey.label = "😀"; // Fallback text if icon missing
        
        try {
            // Try to load emoji icon
            emojiKey.icon = getResources().getDrawable(R.drawable.ic_emoji);
        } catch (Resources.NotFoundException e) {
            // Continue without icon if not found
        }
        
        qwertyKeyboard.getKeys().add(emojiKey);
        
        // Configure keyboard view
        binding.keyboardView.setKeyboard(currentKeyboard);
        binding.keyboardView.setOnKeyboardActionListener(this);
        binding.keyboardView.setPreviewEnabled(true);
    }

    private void setupEmojiKeyboard() {
        if (emojiView == null) {
            // Inflate emoji keyboard layout
            emojiView = View.inflate(this, R.layout.emoji_keyboard, null);
            
            // Get view references
            ViewPager emojiViewPager = emojiView.findViewById(R.id.emojiViewPager);
            TabLayout emojiCategories = emojiView.findViewById(R.id.emojiCategories);
            
            // Set up emoji pager
            emojiViewPager.setAdapter(new EmojiAdapter(emojiManager.getEmojiList(), this));
            emojiCategories.setupWithViewPager(emojiViewPager);
        }
    }

    private void setupSuggestions() {
        // Configure suggestions recycler view
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
                // Handle backspace
                CharSequence before = ic.getTextBeforeCursor(1, 0);
                if (!TextUtils.isEmpty(before)) {
                    ic.deleteSurroundingText(1, 0);
                    if (isPredictionEnabled) {
                        wordComposer.deleteLast();
                        updateSuggestions();
                    }
                }
                break;
                
            case Keyboard.KEYCODE_SHIFT:
                // Toggle shift state
                if (binding.keyboardView.isShifted()) {
                    binding.keyboardView.setShifted(false);
                    capsLock = false;
                } else {
                    binding.keyboardView.setShifted(true);
                    capsLock = true;
                }
                binding.keyboardView.invalidateAllKeys();
                break;
                
            case Keyboard.KEYCODE_MODE_CHANGE:
                // Switch between QWERTY and symbols
                currentKeyboard = (currentKeyboard == qwertyKeyboard) ? symbolsKeyboard : qwertyKeyboard;
                binding.keyboardView.setKeyboard(currentKeyboard);
                break;
                
            case KEYCODE_EMOJI:
                // Toggle emoji keyboard
                isEmojiKeyboard = !isEmojiKeyboard;
                if (isEmojiKeyboard) {
                    setupEmojiKeyboard();
                    setInputView(emojiView);
                } else {
                    setInputView(binding.getRoot());
                }
                break;
                
            case Keyboard.KEYCODE_DONE:
                // Send enter key
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
                
            default:
                // Handle regular character input
                char code = (char) primaryCode;
                if (Character.isLetter(code)) {
                    // Apply shift state to letters
                    code = binding.keyboardView.isShifted() ? Character.toUpperCase(code) : Character.toLowerCase(code);
                    if (!capsLock) binding.keyboardView.setShifted(false);
                }
                
                // Commit character to input
                ic.commitText(String.valueOf(code), 1);
                
                // Update predictions if enabled
                if (isPredictionEnabled && Character.isLetter(code)) {
                    wordComposer.add(code, keyCodes);
                    updateSuggestions();
                } else {
                    suggestions.clear();
                    suggestionAdapter.notifyDataSetChanged();
                }
        }
    }

    private void updateSuggestions() {
        // Get new suggestions from dictionary
        suggestions.clear();
        suggestions.addAll(dictionary.getSuggestions(wordComposer));
        suggestionAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSuggestionClick(String word) {
        // Handle suggestion selection
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            // Replace current word with suggestion
            ic.deleteSurroundingText(wordComposer.size(), 0);
            ic.commitText(word, 1);
            
            // Reset prediction state
            wordComposer.reset();
            suggestions.clear();
            suggestionAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onEmojiClick(String emoji) {
        // Insert selected emoji
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(emoji, 1);
    }

    // Required keyboard action listener methods
    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    
    @Override 
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }
    
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
