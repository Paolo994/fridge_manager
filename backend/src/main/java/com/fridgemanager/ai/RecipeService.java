package com.fridgemanager.ai;

import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fridgemanager.ai.AiModels.RecipeSuggestions;
import com.fridgemanager.food.FoodDtos.FoodResponse;
import com.fridgemanager.food.FoodService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Suggerimento di ricette. Una chiamata sola: il contenuto del frigo è piccolo e va tutto nel prompt,
 * quindi un agente con strumenti aggiungerebbe solo latenza e costo.
 */
@Service
public class RecipeService {

    private final AiConfig ai;
    private final FoodService foods;

    public RecipeService(AiConfig ai, FoodService foods) {
        this.ai = ai;
        this.foods = foods;
    }

    public RecipeSuggestions suggest(Long userId, String preferences) {
        var client = ai.client(); // fallisce subito con 503 se l'IA non è configurata
        List<FoodResponse> items = foods.list(userId);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il frigo è vuoto: aggiungi qualche alimento prima");
        }
        String fridge = items.stream().map(RecipeService::describe).collect(Collectors.joining("\n"));
        String prompt = """
                Suggerisci 3 ricette casalinghe che usino soprattutto quello che c'è nel frigo.
                Dai la priorità agli alimenti che scadono prima (sono in cima alla lista), così da evitare sprechi.
                Non usare gli alimenti segnati come SCADUTO.
                Ogni ricetta può richiedere al massimo 3 ingredienti da comprare. Considera disponibili in dispensa
                sale, pepe, olio, aceto, acqua, farina, zucchero e pasta secca.
                Scrivi tutto in italiano, con passaggi brevi e pratici.

                Contenuto del frigo (ordinato per scadenza):
                %s

                Preferenze dell'utente: %s
                """.formatted(fridge, preferences == null || preferences.isBlank() ? "nessuna" : preferences);

        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(ai.model())
                .maxTokens(16000L);
        ai.applyFallback(builder);
        StructuredMessageCreateParams<RecipeSuggestions> params = builder
                .addUserMessage(prompt)
                .outputConfig(RecipeSuggestions.class)
                .build();

        StructuredMessage<RecipeSuggestions> response = client.messages().create(params);
        if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "L'IA non ha potuto generare ricette");
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .findFirst()
                .map(typed -> typed.text())
                .orElse(new RecipeSuggestions(List.of()));
    }

    private static String describe(FoodResponse f) {
        StringBuilder sb = new StringBuilder("- ").append(f.name());
        if (f.quantity() != null) sb.append(" (").append(f.quantity()).append(f.unit() == null ? "" : " " + f.unit()).append(")");
        if (f.daysToExpiry() != null) {
            long d = f.daysToExpiry();
            sb.append(d < 0 ? " — SCADUTO" : d == 0 ? " — scade oggi" : " — scade tra " + d + " giorni");
        }
        return sb.toString();
    }
}
