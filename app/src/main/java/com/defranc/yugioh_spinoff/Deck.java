package com.defranc.yugioh_spinoff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> deck = new ArrayList<>();

    public Deck() {
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
        deck.add(new TrapCard(
                "Acid Hole",
                R.drawable.acidhole_trap_earth_si0un0monstruo0en0el0campo0es0de0tipo0guerrero0sus0puntos0de0ataque0son0reducidos0en05550puntos,
                "Un monstruo en el campo es de tipo guerrero y sus puntos de ataque son reducidos en 555 puntos",
                MonsterAttribute.EARTH)
        );
        deck.add(new MonsterCard(
                "Akakieisu",
                R.drawable.akakieisu_monster_dark_z_2000_2000_un0hechicero0que0pued0noquear0a0sus0enemigos,
                "Un hechicero que puede noquear a sus enemigos",
                2000,
                2000,
                MonsterType.S,
                MonsterAttribute.DARK
        ));
    }

    public void addCard(Card card) {
        deck.add(card);
    }

    public Card drawCard() {
        if (!deck.isEmpty()) {
            return deck.remove(0);
        }
        return null;
    }

    public boolean isEmpty() {
        return deck.isEmpty();
    }

    public void shuffle() {
        Collections.shuffle(deck);
    }
}