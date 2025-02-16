package com.defranc.yugioh_spinoff;

import android.app.AlertDialog;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class BoardManager {
    // Enforce only one monster per turn
    public static boolean canPlaceMonster = true;

    // Track if the current phase is Main Phase
    // We'll toggle this from MainActivity
    public static boolean isMainPhase = false;

    public static void setupDragListener(View container, boolean isMonsterSlot) {
        container.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DROP:
                    if (!isMainPhase) {
                        return false;
                    }
                    View droppedView = (View) event.getLocalState();
                    Card card = (Card) droppedView.getTag();

                    // Check for null card
                    if (card == null) {
                        return false;
                    }

                    FrameLayout target = (FrameLayout) v;
                    if (target.getChildCount() > 0) {
                        return false;
                    }
                    if (card.isMonsterCard() != isMonsterSlot) {
                        return false;
                    }
                    if (card.isMonsterCard()) {
                        if (!canPlaceMonster) {
                            return false;
                        }
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Position")
                                .setMessage("Place this monster in Attack or Defense position?")
                                .setPositiveButton("Attack", (dialog, which) -> placeCard(target, droppedView, false))
                                .setNegativeButton("Defense", (dialog, which) -> placeCard(target, droppedView, true))
                                .create()
                                .show();
                    } else {
                        placeCard(target, droppedView, false);
                    }
                    return true;

                default:
                    return true;
            }
        });
    }

    private static void placeCard(
            FrameLayout target,
            View droppedView,
            boolean defense
    ) {
        ViewGroup oldParent = (ViewGroup) droppedView.getParent();
        oldParent.removeView(droppedView);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        target.addView(droppedView, params);

        Card card = (Card) droppedView.getTag();
        if (card == null) {
            return; // Ensure tag is not null
        }
        // Once a monster is placed, disallow further monster placements
        if (card.isMonsterCard()) {
            canPlaceMonster = false;
        }

        if (card instanceof MonsterCard) {
            ((MonsterCard) card).changePosition(defense);
        }
        droppedView.setRotation(defense ? 90f : 0f);

        // Disable further dragging
        droppedView.setOnLongClickListener(null);
        droppedView.setOnTouchListener(null);

        MainActivity activity = (MainActivity) target.getContext();
        droppedView.setOnClickListener(v -> {
            if (!activity.isInBattlePhase) {
                Card c = (Card) v.getTag();
                if (c instanceof MonsterCard) {
                    boolean wasDefense = ((MonsterCard) c).isDefense();
                    ((MonsterCard) c).changePosition(!wasDefense);
                    v.setRotation(!wasDefense ? 90f : 0f);
                    new AlertDialog.Builder(activity)
                            .setTitle("Position")
                            .setMessage("This monster is now in " + (!wasDefense ? "Defense" : "Attack") + " position")
                            .setPositiveButton("OK", null)
                            .show();
                }
            } else {
                activity.handleMonsterCardClick(v);
            }
        });
    }
}