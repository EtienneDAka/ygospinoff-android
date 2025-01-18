package com.defranc.yugioh_spinoff;

public class Card {
    private final String name;
    private final String description;
    private final int imageResId;
    private final boolean isMonsterCard;

    public Card(
            String name,
            int imageResId,
            String description,
            boolean isMonsterCard
    ) {
        this.name = name;
        this.imageResId = imageResId;
        this.description = description;
        this.isMonsterCard = isMonsterCard;
    }

    public String getName() { return name; }
    public boolean isMonsterCard() { return isMonsterCard; }
    public String getDescription() { return description; }
    public int getImageResId() { return imageResId; }
}

class MonsterCard extends Card {
    private int attack;
    private int defense;
    private final MonsterType type;
    private final MonsterAttribute attribute;
    private boolean isDefense;

    public MonsterCard (
            String name,
            int imageResId,
            String description,
            int attack,
            int defense,
            MonsterType type,
            MonsterAttribute attribute
    ) {
        super(name, imageResId, description, true);
        this.attack = attack;
        this.defense = defense;
        this.type = type;
        this.attribute = attribute;
        this.isDefense = false;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public MonsterType getType() {
        return type;
    }

    public MonsterAttribute getAttribute() {
        return attribute;
    }

    public boolean isDefense() {
        return isDefense;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public void changePosition (boolean isDefense) {
        this.isDefense = isDefense;
    }
    public void setAttack(int attack) {
        this.attack = attack;
    }
}

class MagicCard extends Card {
    private final MonsterType affectedMonster;
    private final int attackBoost;
    private final int defenseBoost;
    private boolean isActive;

    public MagicCard (
            String name,
            int imageResId,
            String description,
            MonsterType affectedMonster
    ) {
        super(name, imageResId, description, false);
        this.affectedMonster = affectedMonster;
        this.attackBoost = 555;
        this.defenseBoost = 555;
        this.isActive = false;
    }

    public boolean activate(MonsterCard monster) {
        if (monster.getType().getValue().equals(affectedMonster.getValue())) {
            monster.setAttack(monster.getAttack() + attackBoost);
            monster.setDefense(monster.getDefense() + defenseBoost);
            return true;
        }
        return false;
    }
}

class TrapCard extends Card {
    private boolean isActive;
    private MonsterAttribute affectsMonster;

    public TrapCard (
            String name,
            int imageResId,
            String description,
            MonsterAttribute affectsMonster
    ) {
        super(name, imageResId, description, false);
        this.isActive = false;
        this.affectsMonster = affectsMonster;
    }

    public boolean activate(MonsterCard attackingMonster) {
        if (attackingMonster.getAttribute().getValue().equals(affectsMonster.getValue())) {
            isActive = true;
            System.out.println(
                    "La carta trampa '" + super.getName() + "' es activada y niega el ataque de '"
                    + attackingMonster.getName() + "'!");
            return true;
        }
        return false;
    }
}