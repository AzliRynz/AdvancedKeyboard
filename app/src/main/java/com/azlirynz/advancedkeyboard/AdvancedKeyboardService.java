package com.azlirynz.advancedkeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;

import com.azlirynz.advancedkeyboard.dictionary.Dictionary;
import com.azlirynz.advancedkeyboard.dictionary.WordComposer;
import com.azlirynz.advancedkeyboard.emoji.EmojiAdapter;
import com.azlirynz.advancedkeyboard.emoji.EmojiManager;
import com.azlirynz.advancedkeyboard.suggestions.SuggestionAdapter;

public class AdvancedKeyboardService extends InputMethodService 
    implements KeyboardView.OnKeyboardActionListener,
    SuggestionAdapter.OnSuggestionClickListener,
    EmojiAdapter.OnEmojiClickListener {

    private KeyboardLayoutBinding binding;
    private EmojiKeyboardLayoutBinding emojiBinding;
    
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
    
    @Override
    public void onCreate() {
        super.onCreate();
        dictionary = new Dictionary(this);
        emojiManager = new EmojiManager(this);
        loadResources();
    }
    
    private void loadResources() {
        new Thread(() -> {
            dictionary.load();
            emojiManager.load();
        }).start();
    }
    
    @Override
    public View onCreateInputView() {
        binding = KeyboardLayoutBinding.inflate(getLayoutInflater());
        emojiBinding = EmojiKeyboardLayoutBinding.inflate(getLayoutInflater());
        
        setupMainKeyboard();
        setupEmojiKeyboard();
        setupSuggestions();
        
        return binding.getRoot();
    }
    
    private void setupMainKeyboard() {
        qwertyKeyboard = new Keyboard(this, R.xml.qwerty);
        symbolsKeyboard = new Keyboard(this, R.xml.number_symbols);
        currentKeyboard = qwertyKeyboard;
        
        binding.keyboardView.setKeyboard(currentKeyboard);
        binding.keyboardView.setOnKeyboardActionListener(this);
        binding.keyboardView.setPreviewEnabled(true);
    }
    
    private void setupEmojiKeyboard() {
        EmojiAdapter emojiAdapter = new EmojiAdapter(this, emojiManager.getCategories(), this);
        emojiBinding.emojiViewPager.setAdapter(emojiAdapter);
        
        emojiBinding.emojiCategories.setupWithViewPager(emojiBinding.emojiViewPager);
    }
    
    private void setupSuggestions() {
        suggestionAdapter = new SuggestionAdapter(this, suggestions, this);
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
                toggleKeyboard();
                break;
            case Keyboard.KEYCODE_EMOJI:
                toggleEmojiKeyboard();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            default:
                handleCharacter(primaryCode, ic);
        }
    }
    
    private void handleBackspace(InputConnection ic) {
        CharSequence before = ic.getTextBeforeCursor(1, 0);
        if (TextUtils.isEmpty(before)) return;
        
        ic.deleteSurroundingText(1, 0);
        
        if (isPredictionEnabled) {
            wordComposer.deleteLast();
            updateSuggestions();
        }
    }
    
    private void handleShift() {
        if (binding.keyboardView.isShifted()) {
            binding.keyboardView.setShifted(false);
            capsLock = false;
        } else {
            binding.keyboardView.setShifted(true);
            capsLock = true;
        }
        binding.keyboardView.invalidateAllKeys();
    }
    
    private void toggleKeyboard() {
        if (currentKeyboard == qwertyKeyboard) {
            currentKeyboard = symbolsKeyboard;
        } else {
            currentKeyboard = qwertyKeyboard;
        }
        binding.keyboardView.setKeyboard(currentKeyboard);
    }
    
    private void toggleEmojiKeyboard() {
        isEmojiKeyboard = !isEmojiKeyboard;
        if (isEmojiKeyboard) {
            setInputView(emojiBinding.getRoot());
        } else {
            setInputView(binding.getRoot());
        }
    }
    
    private void handleCharacter(int primaryCode, InputConnection ic) {
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
        suggestions.addAll(dictionary.getSuggestions(wordComposer));
        suggestionAdapter.notifyDataSetChanged();
    }
    
    private void clearSuggestions() {
        suggestions.clear();
        suggestionAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onSuggestionClick(String word) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        ic.deleteSurroundingText(wordComposer.size(), 0);
        ic.commitText(word, 1);
        wordComposer.reset();
        clearSuggestions();
    }
    
    @Override
    public void onEmojiClick(String emoji) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(emoji, 1);
        }
    }
    
    // Implement other required methods...
}