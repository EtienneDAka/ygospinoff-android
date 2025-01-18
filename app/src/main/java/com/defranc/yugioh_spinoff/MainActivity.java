package com.defranc.yugioh_spinoff;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipData;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private enum Phase {
        DRAW,
        MAIN,
        BATTLE
    }

    private final List<Card> deck = new ArrayList<>();
    private boolean isFirstTurn = true;
    private Phase currentPhase = Phase.DRAW;
    private Button phaseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        phaseButton = findViewById(R.id.phase_button);

        // Sample cards
        deck.add(new Card("Akakieisu Monster", Card.CardType.MONSTER, R.drawable.akakieisu_monster, "Un hechicero que pronuncia encantamientos que pueden dejar inconsciente a un monstruo."));
        deck.add(new Card("Acid Rain", Card.CardType.MAGIC, R.drawable.acid_rain_magic));
        deck.add(new Card("Alexandrite Dragon Monster", Card.CardType.MONSTER, R.drawable.alexandrite_dragon_monster));
        deck.add(new Card("Alien Shocktrooper Monster", Card.CardType.MONSTER, R.drawable.alien_shocktrooper_monster));

        phaseButton.setText("Draw Phase");
        phaseButton.setOnClickListener(v -> advancePhase());

        // Enable dragging of existing bottom-hand cards
        HorizontalScrollView bottomScrollView = findViewById(R.id.hand1);
        if (bottomScrollView.getChildCount() > 0 && bottomScrollView.getChildAt(0) instanceof LinearLayout) {
            LinearLayout bottomLayout = (LinearLayout) bottomScrollView.getChildAt(0);
            setDragForAllCards(bottomLayout);
        }

        // Prepare board slots
        setupAllowedDrops();
    }

    private void setDragForAllCards(LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ImageView) {
                String resourceName = getResources().getResourceEntryName(child.getId());
                Card card = createCard(resourceName);
                child.setTag(card);
                setupDrag(child);
            }
        }
    }

    private Card createCard(String resourceName) {
        Card.CardType type;
        if (resourceName.contains("_monster")) {
            type = Card.CardType.MONSTER;
        } else if (resourceName.contains("_trap")) {
            type = Card.CardType.TRAP;
        } else if (resourceName.contains("_magic")) {
            type = Card.CardType.MAGIC;
        } else {
            type = Card.CardType.MONSTER;
        }
        int resId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
        return new Card(resourceName, type, resId);
    }

    private void setupAllowedDrops() {
        BoardManager.setupDragListener(findViewById(R.id.player_monster_container_1), true);
        BoardManager.setupDragListener(findViewById(R.id.player_monster_container_2), true);
        BoardManager.setupDragListener(findViewById(R.id.player_monster_container_3), true);
        BoardManager.setupDragListener(findViewById(R.id.player_trap_container_1), false);
        BoardManager.setupDragListener(findViewById(R.id.player_trap_container_2), false);
        BoardManager.setupDragListener(findViewById(R.id.player_trap_container_3), false);
    }

    private void setupDrag(View view) {
        view.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(data, shadowBuilder, v, 0);
            } else {
                v.startDrag(data, shadowBuilder, v, 0);
            }
            return true;
        });
    }

    private void advancePhase() {
        switch (currentPhase) {
            case DRAW:
                // Draw new card
                if (!deck.isEmpty()) {
                    Card newCard = deck.remove(0);
                    ImageView newCardView = new ImageView(this);
                    int width = (int) getResources().getDimension(R.dimen.card_width);
                    int height = (int) getResources().getDimension(R.dimen.card_height);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
                    params.setMargins(8, 8, 8, 8);
                    newCardView.setLayoutParams(params);
                    newCardView.setImageResource(newCard.getImageResId());
                    newCardView.setTag(newCard);
                    setupDrag(newCardView);

                    HorizontalScrollView bottomScrollView = findViewById(R.id.hand1);
                    if (bottomScrollView.getChildCount() > 0 && bottomScrollView.getChildAt(0) instanceof LinearLayout) {
                        LinearLayout bottomLayout = (LinearLayout) bottomScrollView.getChildAt(0);
                        bottomLayout.addView(newCardView);
                    }

                    new AlertDialog.Builder(this)
                            .setMessage("A new card has been added to your hand: " + newCard.getName())
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setMessage("No more cards in the deck.")
                            .setPositiveButton("OK", null)
                            .show();
                }
                currentPhase = Phase.MAIN;
                phaseButton.setText("Main Phase");
                // Enable board drops for monster/trap (one monster per turn)
                BoardManager.canPlaceMonster = true;
                // Indicate we are in Main Phase for BoardManager
                BoardManager.isMainPhase = true;
                break;

            case MAIN:
                // If first turn, end turn; else go to battle
                if (isFirstTurn) {
                    new AlertDialog.Builder(this)
                            .setMessage("Your first turn has ended.")
                            .setPositiveButton("OK", null)
                            .show();
                    endTurn();
                } else {
                    currentPhase = Phase.BATTLE;
                    phaseButton.setText("Battle Phase");
                    // Not main phase anymore
                    BoardManager.isMainPhase = false;
                }
                isFirstTurn = false;
                break;

            case BATTLE:
                // Next turn => go back to draw
                currentPhase = Phase.DRAW;
                phaseButton.setText("Draw Phase");
                BoardManager.isMainPhase = false;
                break;
        }
    }

    private void endTurn() {
        isFirstTurn = false;
        phaseButton.setText("Draw Phase");
        currentPhase = Phase.DRAW;
        // Not main phase
        BoardManager.isMainPhase = false;
    }
}