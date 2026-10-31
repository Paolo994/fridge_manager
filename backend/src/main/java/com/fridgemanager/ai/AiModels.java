package com.fridgemanager.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Schemi di output strutturato: l'SDK ne ricava lo JSON Schema che Claude deve rispettare. */
public final class AiModels {

    private AiModels() {}

    public record RecognizedFood(
            @JsonPropertyDescription("Nome dell'alimento in italiano, breve (es. 'Latte intero', 'Mozzarella')")
            String name,
            @JsonPropertyDescription("Una tra: DAIRY, MEAT, FISH, VEGETABLES, FRUIT, EGGS, BEVERAGES, SAUCES, LEFTOVERS, BAKERY, FROZEN, OTHER")
            String category,
            @JsonPropertyDescription("Quantità stimata; 0 se non stimabile")
            double quantity,
            @JsonPropertyDescription("Unità: pz, g, kg, ml, l, conf; stringa vuota se non nota")
            String unit,
            @JsonPropertyDescription("Data di scadenza ISO yyyy-MM-dd, oppure stringa vuota se non nota")
            String expiryDate,
            @JsonPropertyDescription("true se la data è stata letta dall'etichetta, false se stimata o assente")
            boolean expiryFromLabel,
            @JsonPropertyDescription("Confidenza del riconoscimento tra 0 e 1")
            double confidence) {}

    public record FoodRecognition(
            @JsonPropertyDescription("Alimenti riconosciuti; lista vuota se l'immagine non contiene cibo")
            List<RecognizedFood> items) {}

    public record Recipe(
            String title,
            @JsonPropertyDescription("Una o due frasi di descrizione")
            String description,
            @JsonPropertyDescription("Tempo di preparazione totale in minuti")
            int minutes,
            @JsonPropertyDescription("Ingredienti presi dal frigo, con il nome esatto usato nella lista")
            List<String> usedFridgeItems,
            @JsonPropertyDescription("Ingredienti da comprare (esclusi sale, olio, acqua, pepe)")
            List<String> missingIngredients,
            List<String> steps) {}

    public record RecipeSuggestions(List<Recipe> recipes) {}
}
