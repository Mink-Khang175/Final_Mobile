package com.finalproject.v_league_ticket.presentation.standings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.v_league_ticket.R;
import com.finalproject.v_league_ticket.domain.model.Standing;

import java.util.Locale;

public class StandingsAdapter extends ListAdapter<Standing, StandingsAdapter.StandingViewHolder> {
    public StandingsAdapter() {
        super(DIFF);
    }

    @Override
    public StandingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_standing, parent, false);
        int gap = Math.round(6 * parent.getResources().getDisplayMetrics().density);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, gap);
        view.setLayoutParams(params);
        return new StandingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StandingViewHolder holder, int position) {
        holder.bind(getItem(position), getItemCount());
    }

    static class StandingViewHolder extends RecyclerView.ViewHolder {
        private final TextView positionText;
        private final View zoneIndicator;
        private final ImageView teamLogo;
        private final TextView teamName;
        private final TextView played;
        private final TextView goalDiff;
        private final TextView points;

        StandingViewHolder(ViewGroup itemView) {
            super(itemView);
            positionText = itemView.findViewById(R.id.tvPosition);
            zoneIndicator = itemView.findViewById(R.id.viewZoneIndicator);
            teamLogo = itemView.findViewById(R.id.imgTeamLogo);
            teamName = itemView.findViewById(R.id.tvTeamName);
            played = itemView.findViewById(R.id.tvPlayed);
            goalDiff = itemView.findViewById(R.id.tvGoalDiff);
            points = itemView.findViewById(R.id.tvPoints);
        }

        void bind(Standing standing, int totalTeams) {
            boolean champion = standing.getPosition() == 1;
            boolean continental = standing.getPosition() <= 3;
            boolean relegation = standing.getPosition() >= Math.max(1, totalTeams - 1);

            positionText.setText(champion
                    ? "🏆\n" + String.format(Locale.US, "%02d", standing.getPosition())
                    : String.format(Locale.US, "%02d", standing.getPosition()));
            teamName.setText(standing.getTeamName());
            played.setText(String.valueOf(standing.getPlayed()));
            goalDiff.setText(standing.getGoalDifference() > 0 ? "+" + standing.getGoalDifference() : String.valueOf(standing.getGoalDifference()));
            points.setText(String.valueOf(standing.getPoints()));
            int rankColor = ContextCompat.getColor(itemView.getContext(), R.color.dark_gray_text);
            int zoneColor = ContextCompat.getColor(itemView.getContext(), R.color.transparent);
            if (champion) {
                rankColor = ContextCompat.getColor(itemView.getContext(), R.color.premium_gold);
                zoneColor = ContextCompat.getColor(itemView.getContext(), R.color.premium_gold);
            } else if (continental) {
                rankColor = ContextCompat.getColor(itemView.getContext(), R.color.premium_gold);
            } else if (relegation) {
                rankColor = ContextCompat.getColor(itemView.getContext(), R.color.zone_relegation_red);
                zoneColor = ContextCompat.getColor(itemView.getContext(), R.color.zone_relegation_red);
            }
            positionText.setTextColor(rankColor);
            zoneIndicator.setBackgroundColor(zoneColor);
            Glide.with(teamLogo).load(standing.getLogoUrl().isEmpty() ? null : standing.getLogoUrl())
                    .placeholder(R.drawable.ic_logo).error(R.drawable.ic_logo).centerInside().into(teamLogo);
        }
    }

    private static final DiffUtil.ItemCallback<Standing> DIFF = new DiffUtil.ItemCallback<Standing>() {
        @Override
        public boolean areItemsTheSame(Standing oldItem, Standing newItem) {
            if (oldItem.getTeamId() == null || newItem.getTeamId() == null) {
                return oldItem.getTeamName().equals(newItem.getTeamName());
            }
            return oldItem.getTeamId().equals(newItem.getTeamId());
        }

        @Override
        public boolean areContentsTheSame(Standing oldItem, Standing newItem) {
            return oldItem.equals(newItem);
        }
    };
}
