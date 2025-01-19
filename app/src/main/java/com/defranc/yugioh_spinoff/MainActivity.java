package com.defranc.yugioh_spinoff;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipData;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private enum Phase {
        DRAW,
        MAIN,
        BATTLE
    }

    private final Deck deck = new Deck();
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


        // Initialize Player and Machine
        player = new Player("Player");
        machine = new Machine();



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

        // Enable dragging for existing top-hand cards
        HorizontalScrollView topScrollView = findViewById(R.id.hand2);
        if (topScrollView.getChildCount() > 0 && topScrollView.getChildAt(0) instanceof LinearLayout) {
            LinearLayout topLayout = (LinearLayout) topScrollView.getChildAt(0);
            setTagsForMachineHand(topLayout);
        }
        // Prepare board slots to accept drops
        setupAllowedDrops();
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

    private void setTagsForMachineHand(LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ImageView) {
                String resourceName = getResources().getResourceEntryName(child.getId());
                Log.d("RESOURCENAME", resourceName);
                Card card = createCard(resourceName);
                child.setTag(card);
            }
        }
    }

    private Card createCard(String resourceName) {
        String[] parts = resourceName.split("_");
        Log.d("PARTSSS", Arrays.toString(parts));
        int resID = getResources().getIdentifier(resourceName, "drawable", getPackageName());
        Log.d("RESSID", String.valueOf(resID));
        if (parts.length < 3) {
            return null;
        }

        try {
            String name = parts[0];
            String description = parts[parts.length - 1];
            String formatDescription = description.replaceAll("0", " ");
            Log.d("DESCRIPTION", formatDescription);

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
                    Card newCard = deck.drawCard();
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
                    Toast.makeText(this, "Your first turn has ended.", Toast.LENGTH_SHORT).show();
                    endTurn();
                } else {
                    currentPhase = Phase.BATTLE;
                    phaseButton.setText("Battle Phase");
                    BoardManager.isMainPhase = false;
                }
                isFirstTurn = false;
                break;

            case BATTLE:
                Toast.makeText(this, "Battle phase has ended.", Toast.LENGTH_SHORT).show();
                endTurn();
                break;
        }
    }

    private void advancePhaseMachine() {
        switch (currentPhase) {
            case DRAW:
                if(!deck.isEmpty()) {
                    Card newCard = deck.drawCard();
                    ImageView newCardView = new ImageView(this);
                    int width = (int) getResources().getDimension(R.dimen.card_width);
                    int height = (int) getResources().getDimension(R.dimen.card_height);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
                    params.setMargins(8, 8, 8, 8);
                    newCardView.setLayoutParams(params);
                    newCardView.setImageResource(newCard.getImageResId());
                    newCardView.setTag(newCard);

                    HorizontalScrollView topScrollView = findViewById(R.id.hand2);
                    if (topScrollView.getChildCount() > 0 && topScrollView.getChildAt(0) instanceof LinearLayout) {
                        LinearLayout topLayout = (LinearLayout) topScrollView.getChildAt(0);
                        topLayout.addView(newCardView);
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
                advancePhaseMachine();
                break;

            case MAIN:
                aiPlayCard();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                currentPhase = Phase.BATTLE;
                phaseButton.setText("Battle Phase");
                BoardManager.isMainPhase = false;
                advancePhaseMachine();
                break;

            case BATTLE:
                aiDeclareBattle();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                endTurn();
                break;
        }
    }

    private void aiPlayCard() {
        HorizontalScrollView aiHandScrollView = findViewById(R.id.hand2);
        LinearLayout aiHandLayout = (LinearLayout) aiHandScrollView.getChildAt(0); // asumiendo que el primer hijo es LinearLayout
        int totalCards = aiHandLayout.getChildCount();

        if (totalCards == 0) {
            Log.e("MainActivity", "AI has no cards to play.");
            Toast.makeText(this, "AI has no cards to play.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Seleccionar una carta aleatoria
        int randomIndex = new Random().nextInt(totalCards);
        ImageView selectedCardView = (ImageView) aiHandLayout.getChildAt(randomIndex);
        Card selectedCard = (Card) selectedCardView.getTag();

        if (selectedCard == null) {
            Log.e("MainActivity", "Selected AI card is null.");
            Toast.makeText(this, "AI selected an invalid card.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determinar tipo de carta y encontrar contenedor correspondiente
        FrameLayout targetContainer = null;
        if (selectedCard.isMonsterCard()) {
            targetContainer = findEmptyContainer(new int[]{
                    R.id.machine_monster_container_1,
                    R.id.machine_monster_container_2,
                    R.id.machine_monster_container_3
            });
            if (targetContainer == null) {
                Log.e("MainActivity", "No empty monster containers available for AI to play.");
                Toast.makeText(this, "AI has no place to deploy a monster.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        else  {
            targetContainer = findEmptyContainer(new int[]{
                    R.id.machine_trap_container_1,
                    R.id.machine_trap_container_2,
                    R.id.machine_trap_container_3
            });
            if (targetContainer == null) {
                Log.e("MainActivity", "No empty trap containers available for AI to play.");
                Toast.makeText(this, "AI has no place to deploy a trap.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Mover la carta al contenedor objetivo
        if (targetContainer != null) {
            moveCardToContainer(selectedCardView, aiHandLayout, targetContainer);
            Toast.makeText(this, "AI has played: " + selectedCard.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void aiDeclareBattle() {
        FrameLayout[] playerMonsterContainers = new FrameLayout[]{
                findViewById(R.id.player_monster_container_1),
                findViewById(R.id.player_monster_container_2),
                findViewById(R.id.player_monster_container_3)
        };

        FrameLayout[] machineMonsterContainers = new FrameLayout[]{
                findViewById(R.id.machine_monster_container_1),
                findViewById(R.id.machine_monster_container_2),
                findViewById(R.id.machine_monster_container_3)
        };

        for (FrameLayout machineContainer : machineMonsterContainers) {
            if (machineContainer.getChildCount() > 0) {
                ImageView machineMonsterView = (ImageView) machineContainer.getChildAt(0);
                MonsterCard machineMonster = (MonsterCard) machineMonsterView.getTag();

                for (FrameLayout playerContainer : playerMonsterContainers) {
                    if (playerContainer.getChildCount() > 0) {
                        ImageView playerMonsterView = (ImageView) playerContainer.getChildAt(0);
                        MonsterCard playerMonster = (MonsterCard) playerMonsterView.getTag();

                        if (machineMonster.getAttack() > playerMonster.getDefense()) {
                            player.takeDamage(machineMonster.getAttack() - playerMonster.getDefense());
                            playerContainer.removeView(playerMonsterView);
                            Toast.makeText(this, "AI's " + machineMonster.getName() + " destroyed " + playerMonster.getName(), Toast.LENGTH_SHORT).show();
                        } else {
                            machine.takeDamage(playerMonster.getDefense() - machineMonster.getAttack());
                            machineContainer.removeView(machineMonsterView);
                            Toast.makeText(this, "Player's " + playerMonster.getName() + " destroyed " + machineMonster.getName(), Toast.LENGTH_SHORT).show();
                        }
                        updateLPDisplay();
                        break;
                    }
                }
            }
        }
    }

    private FrameLayout findEmptyContainer(int[] containerIds) {
        for (int id : containerIds) {
            FrameLayout container = findViewById(id);
            if (container != null && container.getChildCount() == 0) {
                return container;
            }
        }
        return null;
    }

    private void moveCardToContainer(ImageView cardView, LinearLayout currentHand, FrameLayout targetContainer) {
        // Remover de la mano actual
        currentHand.removeView(cardView);

        // Agregar al contenedor objetivo
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        targetContainer.addView(cardView, params);

        // Opcional: Aplicar transformaciones, por ejemplo, rotación si está en posición de defensa
        if (cardView.getTag() instanceof MonsterCard) {
            // Por ejemplo, establecer en posición de ataque (0 rotación)
            cardView.setRotation(0f);
        }
        else if (cardView.getTag() instanceof TrapCard) {
            // Las trampas podrían no necesitar rotación
            cardView.setRotation(0f);
        }

        // Deshabilitar el drag para la carta de la máquina
        cardView.setOnLongClickListener(null);
        cardView.setOnTouchListener(null);

        // Opcional: Configurar click listener para activar efectos
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
        advancePhaseMachine();
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
