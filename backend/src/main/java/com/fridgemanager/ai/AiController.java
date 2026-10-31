package com.fridgemanager.ai;

import com.fridgemanager.ai.AiModels.RecipeSuggestions;
import com.fridgemanager.ai.AiModels.RecognizedFood;
import com.fridgemanager.ai.KitchenAssistantAgent.ChatResult;
import com.fridgemanager.ai.KitchenAssistantAgent.ChatTurn;
import com.fridgemanager.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    public record RecipeRequest(@Size(max = 300) String preferences) {}

    public record ChatRequest(@NotBlank @Size(max = 2000) String message, @Size(max = 30) List<ChatTurn> history) {}

    private final AiConfig ai;
    private final VisionService vision;
    private final RecipeService recipes;
    private final KitchenAssistantAgent assistant;

    public AiController(AiConfig ai, VisionService vision, RecipeService recipes, KitchenAssistantAgent assistant) {
        this.ai = ai;
        this.vision = vision;
        this.recipes = recipes;
        this.assistant = assistant;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("enabled", ai.enabled());
    }

    /** Foto di un singolo alimento o confezione: riconosce il prodotto e legge la scadenza. */
    @PostMapping(value = "/recognize-food", consumes = "multipart/form-data")
    public List<RecognizedFood> recognizeFood(@RequestParam("photo") MultipartFile photo) {
        return vision.recognizeFood(photo);
    }

    /** Foto dell'interno del frigo: elenca gli alimenti visibili. */
    @PostMapping(value = "/scan-fridge", consumes = "multipart/form-data")
    public List<RecognizedFood> scanFridge(@RequestParam("photo") MultipartFile photo) {
        return vision.scanFridge(photo);
    }

    @PostMapping("/recipes")
    public RecipeSuggestions recipes(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody RecipeRequest req) {
        return recipes.suggest(user.id(), req.preferences());
    }

    @PostMapping("/assistant")
    public ChatResult assistant(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChatRequest req) {
        return assistant.chat(user.id(), req.history() == null ? List.of() : req.history(), req.message());
    }
}
