package com.defranc.yugioh_spinoff;

public class Card {

    public enum CardType {
        MONSTER,
        TRAP,
        MAGIC
    }

    public enum Orientation {
        ATTACK,
        DEFENSE
    }

    private String name;
    private CardType cardType;
    private String description;
    private Orientation orientation;
    private int imageResId;

    public Card(String name, CardType cardType, int imageResId, String description) {
        this.name = name;
        this.cardType = cardType;
        this.imageResId = imageResId;
        this.description = description;
        this.orientation = Orientation.ATTACK; // Default
    }

    public String getName() { return name; }
    public CardType getCardType() { return cardType; }
    public Orientation getOrientation() { return orientation; }
    public int getImageResId() { return imageResId; }

    public boolean isMonsterCard() {
        return cardType == CardType.MONSTER;
    }

    public boolean isDefense() {
        return orientation == Orientation.DEFENSE;
    }

    public void setDefense(boolean defense) {
        orientation = defense ? Orientation.DEFENSE : Orientation.ATTACK;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }
}

class MonsterCard extends Card {
    private int attack;
    private int defense;
    private MonsterType monster_type;
    private MonsterAttribute attribute;

    public MonsterCard(String name, CardType cardType, int imageResId, String description, int attack, int defense, MonsterType monster_type, MonsterAttribute attribute) {
        super(name, cardType, imageResId, description);
        this.attack = attack;
        this.defense = defense;
        this.monster_type = monster_type;
        this.attribute = attribute;
    }
}