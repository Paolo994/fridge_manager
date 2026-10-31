package com.fridgemanager.food;

public enum FoodCategory {
    DAIRY,
    MEAT,
    FISH,
    VEGETABLES,
    FRUIT,
    EGGS,
    BEVERAGES,
    SAUCES,
    LEFTOVERS,
    BAKERY,
    FROZEN,
    OTHER;

    /** Converte il testo restituito dall'IA, tollerando valori sconosciuti. */
    public static FoodCategory parse(String value) {
        if (value == null) return OTHER;
        try {
            return FoodCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
