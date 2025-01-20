package com.defranc.yugioh_spinoff;

import java.util.ArrayList;

public class Player {
    protected Deck deck;
    protected String name;
    protected ArrayList<Card> hand;
    protected int life = 4000;

//   CONSTRUCTOR
    public Player(String name) {
        this.name = name;
        this.deck = new Deck();
    }

    public int getLife(){
        return life;
    }

    public void drawCard(){
//        hand.add(deck.drawCard(this.deck));
//        MOSTRAR EL ROBO DE CARTA
    }

    public void takeDamage(int dmg){
        life -= dmg;
//        MOSTRAR LA PÉRDIDA DE VIDA
    }

    public void setLife(int life){
        this.life = life;
    }
}

//HACER MACHINE EXTENDS PLAYER Y COMPLETAR EL CONSTRUCTOR Y LOS MÉTODOS
class Machine extends Player {
    public Machine() {
        super("Machine");
    }

    public void drawCard(){
//        hand.add(deck.drawCard(this.deck));
//        MOSTRAR EL ROBO DE CARTA
    }
}
