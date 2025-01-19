package com.defranc.yugioh_spinoff;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipData;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private enum Phase {
        DRAW,
        MAIN,
        BATTLE
    }

    private final List<Card> deck = new ArrayList<>();
    private boolean isFirstTurn = true;
    private boolean isPlayerTurn = true;
    private int totalTurns = 0;
    private Phase currentPhase = Phase.DRAW;
    private Button phaseButton;
    private TextView turnInfoTextView;
    private Player player;
    private Machine machine;
    private TextView playerLPTextView;
    private TextView machineLPTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        phaseButton = findViewById(R.id.phase_button);
        turnInfoTextView = findViewById(R.id.turn_info_text_view);

        // Populate the deck
        populateDeck();

        // Initialize Player and Machine
        player = new Player("Player");
        machine = new Machine("Machine");

        // Link TextViews from the layout
        playerLPTextView = findViewById(R.id.player_lp_text_view);
        machineLPTextView = findViewById(R.id.machine_lp_text_view);

        // Set initial LP values
        updateLPDisplay();

        // Set initial phase button text and update turn info
        phaseButton.setText("Draw Phase");
        updateTurnInfo();

        // Set up phase advancement button click listener
        phaseButton.setOnClickListener(v -> advancePhase());

        // Enable dragging for existing bottom-hand cards
        HorizontalScrollView bottomScrollView = findViewById(R.id.hand1);
        if (bottomScrollView.getChildCount() > 0 && bottomScrollView.getChildAt(0) instanceof LinearLayout) {
            LinearLayout bottomLayout = (LinearLayout) bottomScrollView.getChildAt(0);
            setDragForAllCards(bottomLayout);
        }

        // Prepare board slots to accept drops
        setupAllowedDrops();
    }

    private void populateDeck() {
        deck.add(new MonsterCard(
                "Colored Fish",
                R.drawable.coloredfish_monster_water_b_2750_2000_anaconda0que0reside0en0las0profundidades0de0los0mares0en0busqueda0de0presas0desprevenidas,
                "Anaconda que reside en las profundidades de los mares en busqueda de presas desprevenidas",
                2750,
                2000,
                MonsterType.B,
                MonsterAttribute.WATER)
        );
        deck.add(new MonsterCard(
                "alinsection",
                R.drawable.alinsection_monster_earth_w_2000_1500_un0insecto0con0brazos0y0cabeza0aserrados0necesarios0para0desgarrar0a0sus0victimas0en0un0duelo,
                "Un insecto con brazos y cabeza aserrados necesarios para desgarrar a sus victimas en un duelo",
                2000,
                1500,
                MonsterType.B,
                MonsterAttribute.EARTH)
        );
    }

    private void setDragForAllCards(LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ImageView) {
                String resourceName = getResources().getResourceEntryName(child.getId());
                Log.d("RESOURCENAME", resourceName);
                Card card = createCard(resourceName);
                child.setTag(card);
                setupDrag(child);
            }
        }
    }

    private Card createCard(String resourceName) {
        String[] parts = resourceName.split("_");
        Log.d("PARTSSS", Arrays.toString(parts));
        int resID = getResources().getIdentifier(resourceName, "drawable", getPackageName());
        Log.d("RESSID", String.valueOf(resID));
        if (parts.length < 6) {
            return null;
        }

        try {
            String name = parts[0];
            String description = parts[parts.length - 1];

            if (resourceName.contains("_monster")) {
                MonsterAttribute attribute = MonsterAttribute.valueOf(parts[2].toUpperCase());
                MonsterType type = MonsterType.valueOf(parts[3].toUpperCase());
                int attack = Integer.parseInt(parts[4]);
                int defense = Integer.parseInt(parts[5]);
                return new MonsterCard(name, resID, description, attack, defense, type, attribute);

            } else if (resourceName.contains("_trap")) {
                MonsterAttribute attribute = MonsterAttribute.valueOf(parts[2].toUpperCase());
                return new TrapCard(name, resID, description, attribute);

            } else if (resourceName.contains("_magic")) {
                MonsterType type = MonsterType.valueOf(parts[2].toUpperCase());
                return new MagicCard(name, resID, description, type);

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return null;
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
                BoardManager.canPlaceMonster = true;
                BoardManager.isMainPhase = true;
                break;

            case MAIN:
                if (isFirstTurn) {
                    new AlertDialog.Builder(this)
                            .setMessage("Your first turn has ended.")
                            .setPositiveButton("OK", null)
                            .show();
                    endTurn();
                } else {
                    currentPhase = Phase.BATTLE;
                    phaseButton.setText("Battle Phase");
                    BoardManager.isMainPhase = false;
                }
                isFirstTurn = false;
                break;

            case BATTLE:
                new AlertDialog.Builder(this)
                        .setMessage("Battle phase has ended.")
                        .setPositiveButton("OK", null)
                        .show();

                endTurn();
                break;
        }
    }

    private void endTurn() {
        isPlayerTurn = !isPlayerTurn;
        totalTurns++;
        currentPhase = Phase.DRAW;
        phaseButton.setText("Draw Phase");
        BoardManager.isMainPhase = false;

        updateTurnInfo();

        if (!isPlayerTurn) {
            handleMachineTurn();
        }
    }

    private void handleMachineTurn() {
        if (!deck.isEmpty()) {
            Card newCard = deck.remove(0);
            Log.d("MachineTurn", "Machine drew: " + newCard.getName());
        }

        new AlertDialog.Builder(this)
                .setMessage("Machine's turn has ended.")
                .setPositiveButton("OK", null)
                .show();
        endTurn();
    }

    private void updateTurnInfo() {
        String player = isPlayerTurn ? "Player" : "Machine";
        turnInfoTextView.setText(String.format("Turn: %d\nCurrent: %s", totalTurns, player));
    }

    private void updateLPDisplay() {
        playerLPTextView.setText("Player LP: " + player.getLife());
        machineLPTextView.setText("Machine LP: " + machine.getLife());
    }

    // Example method to simulate damage
    public void simulateDamage() {
        player.takeDamage(500);
        machine.takeDamage(300);
        updateLPDisplay(); // Refresh LP display
    }
}
