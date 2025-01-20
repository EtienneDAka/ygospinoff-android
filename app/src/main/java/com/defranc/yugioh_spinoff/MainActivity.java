package com.defranc.yugioh_spinoff;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipData;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
        BATTLE,
        END_TURN
    }

    private final Deck deck = new Deck();
    private boolean isFirstTurn = true;
    private boolean isPlayerTurn = true;
    private int totalTurns = 1;
    private Phase currentPhase = Phase.DRAW;
    private Button phaseButton;
    private MonsterCard selectedAttacker = null;
    public boolean isInBattlePhase = false;
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

        // Set up click listeners for player monsters
        setupPlayerMonsterClickListeners();
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

    private void setupPlayerMonsterClickListeners() {
        FrameLayout[] playerMonsterContainers = new FrameLayout[]{
                findViewById(R.id.player_monster_container_1),
                findViewById(R.id.player_monster_container_2),
                findViewById(R.id.player_monster_container_3)
        };

        for (FrameLayout container : playerMonsterContainers) {
            if (container.getChildCount() > 0) {
                View monsterView = container.getChildAt(0);
                monsterView.setOnClickListener(v -> handleMonsterCardClick(v));
            }
        }
    }

    public void handleMonsterCardClick(View view) {
        if (!isInBattlePhase) {
            // Allow position change only during Main Phase
            Card c = (Card) view.getTag();
            if (c instanceof MonsterCard) {
                boolean wasDefense = ((MonsterCard) c).isDefense();
                ((MonsterCard) c).changePosition(!wasDefense);
                view.setRotation(!wasDefense ? 90f : 0f);
                new AlertDialog.Builder(this)
                        .setTitle("Position Change")
                        .setMessage("This monster is now in " + (!wasDefense ? "Defense" : "Attack") + " position.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        } else {
            MonsterCard attacker = (MonsterCard) view.getTag();
            if (attacker == null || attacker.isDefense()) {
                Toast.makeText(this, "Cannot attack with a monster in Defense position.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (attacker.hasAttacked()) {
                Toast.makeText(this, "This monster has already attacked this turn.", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedAttacker = attacker;
            showAttackTargetsDialog(view);
        }
    }

    private void showAttackTargetsDialog(View attackerView) {
        FrameLayout[] machineMonsterContainers = new FrameLayout[]{
                findViewById(R.id.machine_monster_container_1),
                findViewById(R.id.machine_monster_container_2),
                findViewById(R.id.machine_monster_container_3)
        };

        boolean machineHasMonsters = false;
        for (FrameLayout container : machineMonsterContainers) {
            if (container.getChildCount() > 0) {
                machineHasMonsters = true;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Attack Target");

        if (machineHasMonsters) {
            View customLayout = getLayoutInflater().inflate(R.layout.attack_targets_popup, null);
            builder.setView(customLayout);

            LinearLayout targetsList = customLayout.findViewById(R.id.targets_list);

            for (FrameLayout container : machineMonsterContainers) {
                if (container.getChildCount() > 0) {
                    View targetView = container.getChildAt(0);
                    MonsterCard targetMonster = (MonsterCard) targetView.getTag();

                    View targetItem = getLayoutInflater().inflate(R.layout.target_item, null);
                    TextView monsterName = targetItem.findViewById(R.id.monster_name);
                    TextView monsterStats = targetItem.findViewById(R.id.monster_stats);

                    monsterName.setText(targetMonster.getName());
                    String stats = "ATK: " + targetMonster.getAttack() + " / DEF: " + targetMonster.getDefense();
                    monsterStats.setText(stats);

                    targetItem.setOnClickListener(v -> {
                        executeAttack(attackerView, targetView);
                        builder.create().dismiss();
                    });

                    targetsList.addView(targetItem);
                }
            }
        } else {
            // Direct attack
            builder.setMessage("No monsters on opponent's field. Attack directly?");
            builder.setPositiveButton("Attack", (dialog, which) -> {
                executeDirectAttack(selectedAttacker);
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                selectedAttacker = null;
            });
        }

        if (machineHasMonsters) {
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                selectedAttacker = null;
            });
        }

        builder.create().show();
    }

    private void executeAttack(View attackerView, View targetView) {
        MonsterCard attacker = (MonsterCard) attackerView.getTag();
        MonsterCard target = (MonsterCard) targetView.getTag();

        if (attacker != null) {
            attacker.setHasAttacked(true); // Mark as attacked
        }

        String targetPosition = target.isDefense() ? "Defense" : "Attack";
        int targetPoints = target.isDefense() ? target.getDefense() : target.getAttack();

        if (attacker.getAttack() > targetPoints) {
            machine.takeDamage(attacker.getAttack() - targetPoints);
            ((ViewGroup) targetView.getParent()).removeView(targetView);
            showBattleResultPopup(attacker, target, "Attack", targetPosition, false);
        } else if (attacker.getAttack() < targetPoints) {
            player.takeDamage(targetPoints - attacker.getAttack());
            ((ViewGroup) attackerView.getParent()).removeView(attackerView);
            showBattleResultPopup(target, attacker, targetPosition, "Attack", true);
        } else {
            // Both monsters are destroyed
            ((ViewGroup) targetView.getParent()).removeView(targetView);
            ((ViewGroup) attackerView.getParent()).removeView(attackerView);
            showBattleResultPopup(attacker, target, "Attack", targetPosition, false);
        }

        updateLPDisplay();
        selectedAttacker = null;
    }

    private void executeDirectAttack(MonsterCard attacker) {
        machine.takeDamage(attacker.getAttack());
        showDirectAttackPopup(attacker);
        updateLPDisplay();
        selectedAttacker = null;
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

    // Modify advancePhase method
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
                isInBattlePhase = false;
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
                    isInBattlePhase = true;
                    Toast.makeText(this, "Battle Phase: Click your monster in Attack position to declare an attack.", Toast.LENGTH_LONG).show();
                }
                isFirstTurn = false;
                break;

            case BATTLE:
                currentPhase = Phase.END_TURN;
                phaseButton.setText("End Turn");
                isInBattlePhase = false;
                // No action needed here as battle interactions are handled via UI
                break;

            case END_TURN:
                Toast.makeText(this, "Battle phase has ended.", Toast.LENGTH_SHORT).show();
                endTurn();
                break;
        }
    }

    private void handlePlayerBattle() {
        FrameLayout[] enemyMonsterContainers = new FrameLayout[]{
                findViewById(R.id.machine_monster_container_1),
                findViewById(R.id.machine_monster_container_2),
                findViewById(R.id.machine_monster_container_3)
        };

        FrameLayout[] playerMonsterContainers = new FrameLayout[]{
                findViewById(R.id.player_monster_container_1),
                findViewById(R.id.player_monster_container_2),
                findViewById(R.id.player_monster_container_3)
        };

        boolean enemyHasMonsters = false;

        // Check if the opponent has monsters
        for (FrameLayout enemyContainer : enemyMonsterContainers) {
            if (enemyContainer.getChildCount() > 0) {
                enemyHasMonsters = true;
                break;
            }
        }

        if (enemyHasMonsters) {
            // Allow the player to select a monster to attack
            showAttackSelectionDialog(playerMonsterContainers, enemyMonsterContainers);
        } else {
            // Perform a direct attack on the opponent
            performDirectAttack(playerMonsterContainers);
        }
    }

    private void performDirectAttack(FrameLayout[] playerMonsterContainers) {
        for (FrameLayout playerContainer : playerMonsterContainers) {
            if (playerContainer.getChildCount() > 0) {
                ImageView playerMonsterView = (ImageView) playerContainer.getChildAt(0);
                MonsterCard playerMonster = (MonsterCard) playerMonsterView.getTag();

                if (playerMonster != null) {
                    machine.takeDamage(playerMonster.getAttack());
                    Toast.makeText(this, playerMonster.getName() + " attacked directly!", Toast.LENGTH_SHORT).show();
                    updateLPDisplay();
                    return; // Only allow one monster to attack directly per turn
                }
            }
        }
    }

    private void showAttackSelectionDialog(FrameLayout[] playerContainers, FrameLayout[] enemyContainers) {
        for (FrameLayout playerContainer : playerContainers) {
            if (playerContainer.getChildCount() > 0) {
                ImageView playerMonsterView = (ImageView) playerContainer.getChildAt(0);
                MonsterCard playerMonster = (MonsterCard) playerMonsterView.getTag();

                for (FrameLayout enemyContainer : enemyContainers) {
                    if (enemyContainer.getChildCount() > 0) {
                        ImageView enemyMonsterView = (ImageView) enemyContainer.getChildAt(0);
                        MonsterCard enemyMonster = (MonsterCard) enemyMonsterView.getTag();

                        // Resolve the battle
                        resolveBattle(playerMonster, enemyMonster, playerContainer, enemyContainer);
                        return;
                    }
                }
            }
        }
    }

    private void resolveBattle(MonsterCard playerMonster, MonsterCard enemyMonster,
                               FrameLayout playerContainer, FrameLayout enemyContainer) {
        int playerPoints = playerMonster.isDefense() ? playerMonster.getDefense() : playerMonster.getAttack();
        int enemyPoints = enemyMonster.isDefense() ? enemyMonster.getDefense() : enemyMonster.getAttack();

        if (playerPoints > enemyPoints) {
            enemyContainer.removeViewAt(0); // Remove the enemy monster
            machine.takeDamage(playerPoints - enemyPoints);
            Toast.makeText(this, playerMonster.getName() + " destroyed " + enemyMonster.getName(), Toast.LENGTH_SHORT).show();
        } else if (playerPoints < enemyPoints) {
            playerContainer.removeViewAt(0); // Remove the player monster
            player.takeDamage(enemyPoints - playerPoints);
            Toast.makeText(this, playerMonster.getName() + " was destroyed by " + enemyMonster.getName(), Toast.LENGTH_SHORT).show();
        } else {
            // Both monsters are destroyed
            playerContainer.removeViewAt(0);
            enemyContainer.removeViewAt(0);
            Toast.makeText(this, playerMonster.getName() + " and " + enemyMonster.getName() + " destroyed each other!", Toast.LENGTH_SHORT).show();
        }

        updateLPDisplay();
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

                    Toast.makeText(this, "A new card has been added to your hand: " + newCard.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No more cards in the deck.", Toast.LENGTH_SHORT).show();
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

        boolean playerHasMonsters = false;

        for (FrameLayout playerContainer : playerMonsterContainers) {
            if (playerContainer.getChildCount() > 0) {
                playerHasMonsters = true;
                break;
            }
        }

        for (FrameLayout machineContainer : machineMonsterContainers) {
            if (machineContainer.getChildCount() > 0) {
                ImageView machineMonsterView = (ImageView) machineContainer.getChildAt(0);
                MonsterCard machineMonster = (MonsterCard) machineMonsterView.getTag();

                if (playerHasMonsters) {
                    for (FrameLayout playerContainer : playerMonsterContainers) {
                        if (playerContainer.getChildCount() > 0) {
                            ImageView playerMonsterView = (ImageView) playerContainer.getChildAt(0);
                            MonsterCard playerMonster = (MonsterCard) playerMonsterView.getTag();

                            String machineMonsterPosition = machineMonster.isDefense() ? "defense" : "attack";
                            String playerMonsterPosition = playerMonster.isDefense() ? "defense" : "attack";
                            int playerMonsterPoints = playerMonster.isDefense() ? playerMonster.getDefense() : playerMonster.getAttack();

                            if (machineMonster.getAttack() > playerMonsterPoints) {
                                player.takeDamage(machineMonster.getAttack() - playerMonsterPoints);
                                playerContainer.removeView(playerMonsterView);

                                Log.d("MACHINEWIN", machineMonster.getAttack() - playerMonsterPoints + "");
                                showBattleResultPopup(machineMonster, playerMonster, machineMonsterPosition, playerMonsterPosition, true);
                            } else {
                                Log.d("PLAYERRSWIN", playerMonsterPoints - machineMonster.getAttack() + "");
                                machine.takeDamage(playerMonsterPoints - machineMonster.getAttack());
                                machineContainer.removeView(machineMonsterView);

                                showBattleResultPopup(machineMonster, playerMonster, machineMonsterPosition, playerMonsterPosition, false);
                            }
                            updateLPDisplay();
                            break;
                        }
                    }
                } else {
                    // Attack directly to the player
                    player.takeDamage(machineMonster.getAttack());
                    showDirectAttackPopup(machineMonster);
                    updateLPDisplay();
                }
            }
        }
    }


    private void resetMonsterFlags() {
        FrameLayout[] playerMonsterContainers = new FrameLayout[]{
                findViewById(R.id.player_monster_container_1),
                findViewById(R.id.player_monster_container_2),
                findViewById(R.id.player_monster_container_3)
        };

        for (FrameLayout container : playerMonsterContainers) {
            if (container.getChildCount() > 0) {
                MonsterCard monster = (MonsterCard) container.getChildAt(0).getTag();
                if (monster != null) {
                    monster.setHasAttacked(false);
                }
            }
        }
    }


    private void showBattleResultPopup(MonsterCard attacker, MonsterCard target, String attackerPosition, String targetPosition, boolean targetWins) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customLayout = getLayoutInflater().inflate(R.layout.battle_result_popup, null);
        builder.setView(customLayout);

        TextView attackerName = customLayout.findViewById(R.id.attacker_name);
        TextView attackerStats = customLayout.findViewById(R.id.attacker_stats);
        TextView targetName = customLayout.findViewById(R.id.target_name);
        TextView targetStats = customLayout.findViewById(R.id.target_stats);
        TextView battleOutcome = customLayout.findViewById(R.id.battle_outcome);

        attackerName.setText(attacker.getName() + " (" + attackerPosition + ")");
        attackerStats.setText("ATK: " + attacker.getAttack() + " / DEF: " + attacker.getDefense());
        targetName.setText(target.getName() + " (" + targetPosition + ")");
        targetStats.setText("ATK: " + target.getAttack() + " / DEF: " + target.getDefense());

        if (targetWins) {
            battleOutcome.setText("The opponent's " + target.getName() + " destroyed your " + attacker.getName());
        } else {
            battleOutcome.setText("Your " + attacker.getName() + " destroyed the opponent's " + target.getName());
        }

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showDirectAttackPopup(MonsterCard attacker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customLayout = getLayoutInflater().inflate(R.layout.direct_attack_popup, null);
        builder.setView(customLayout);

        TextView attackerName = customLayout.findViewById(R.id.attacker_name);
        TextView attackerStats = customLayout.findViewById(R.id.attacker_stats);
        TextView directAttackOutcome = customLayout.findViewById(R.id.direct_attack_outcome);

        attackerName.setText(attacker.getName() + " (Attack)");
        attackerStats.setText("ATK: " + attacker.getAttack());

        directAttackOutcome.setText("Your " + attacker.getName() + " attacked directly!");

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.create().show();
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
        resetMonsterFlags(); // Reset attack flags for all monsters

        updateTurnInfo();

        if (player.getLife() <= 0) {
            player.setLife(0);
            Toast.makeText(this, "You lost the game!", Toast.LENGTH_SHORT).show();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            endGame();
            return;
        } else if (machine.getLife() <= 0) {
            machine.setLife(0);
            Toast.makeText(this, "You won the game!", Toast.LENGTH_SHORT).show();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            endGame();
            return;
        }

        if (!isPlayerTurn) {
            handleMachineTurn();
        }
    }

    public void endGame() {
        Toast.makeText(this, "Game Over!", Toast.LENGTH_SHORT).show();
        finish();
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
}







