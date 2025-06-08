package com.azlirynz.advancedkeyboard.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.azlirynz.advancedkeyboard.R;

import java.util.List;

public class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder> {
    public interface OnEmojiClickListener {
        void onEmojiClick(String emoji);
    }

    private final List<String> emojis;
    private final OnEmojiClickListener listener;

    public EmojiAdapter(@NonNull List<String> emojis, @NonNull OnEmojiClickListener listener) {
        this.emojis = emojis;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.emoji_item, parent, false);
        return new EmojiViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
        holder.bind(emojis.get(position));
    }

    @Override
    public int getItemCount() {
        return emojis.size();
    }

    static class EmojiViewHolder extends RecyclerView.ViewHolder {
        private final TextView emojiView;
        private final OnEmojiClickListener listener;

        EmojiViewHolder(@NonNull View itemView, @NonNull OnEmojiClickListener listener) {
            super(itemView);
            this.listener = listener;
            emojiView = itemView.findViewById(R.id.emoji_text);
        }

        void bind(@NonNull final String emoji) {
            emojiView.setText(emoji);
            itemView.setOnClickListener(v -> listener.onEmojiClick(emoji));
        }
    }
}
