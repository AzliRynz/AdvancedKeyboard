package com.azlirynz.advancedkeyboard.suggestions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.azlirynz.advancedkeyboard.R;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {
    public interface OnSuggestionClickListener {
        void onSuggestionClick(String word);
    }

    private final List<String> suggestions;
    private final OnSuggestionClickListener listener;

    public SuggestionAdapter(List<String> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.suggestion_item, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        holder.bind(suggestions.get(position));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private final TextView suggestionText;

        SuggestionViewHolder(View itemView) {
            super(itemView);
            suggestionText = itemView.findViewById(R.id.suggestion_text);
        }

        void bind(final String suggestion) {
            suggestionText.setText(suggestion);
            itemView.setOnClickListener(v -> listener.onSuggestionClick(suggestion));
        }
    }
}