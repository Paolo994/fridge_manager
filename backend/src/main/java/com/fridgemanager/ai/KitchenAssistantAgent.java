package com.fridgemanager.ai;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fridgemanager.food.FoodCategory;
import com.fridgemanager.food.FoodDtos.FoodRequest;
import com.fridgemanager.food.FoodDtos.FoodResponse;
import com.fridgemanager.food.FoodService;
import com.fridgemanager.food.FoodSource;
import com.fridgemanager.shopping.ShoppingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assistente di cucina: è l'unico punto dove un agente ha senso, perché la richiesta è aperta
 * ("ho finito il latte e stasera voglio fare una torta") e Claude decide quali strumenti usare
 * e in che ordine. Gli strumenti lavorano solo sui dati dell'utente autenticato.
 * Il ciclo è scritto a mano per avere il controllo su limite di passi e log delle azioni.
 */
@Service
public class KitchenAssistantAgent {

    private static final Logger log = LoggerFactory.getLogger(KitchenAssistantAgent.class);
    private static final int MAX_STEPS = 8;

    public record ChatTurn(String role, String text) {}

    public record ChatResult(String reply, List<String> actions) {}

    private static final String SYSTEM = """
            Sei l'assistente di cucina dell'app Fridge Manager. Rispondi in italiano, in modo breve e pratico.
            La chat mostra testo semplice: non usare markdown (niente asterischi, titoli o tabelle);
            per gli elenchi usa righe che iniziano con "- ".
            Usa gli strumenti per leggere e modificare il frigo e la lista della spesa dell'utente:
            non inventare il contenuto del frigo, leggilo con list_fridge_items.
            Quando l'utente dice di aver finito, consumato o buttato qualcosa, rimuovilo dal frigo;
            se è un alimento che probabilmente ricomprerà, aggiungilo anche alla lista della spesa.
            Prima di rimuovere un alimento ambiguo (più voci con nome simile) chiedi quale.
            Alla fine riassumi in una frase le modifiche che hai fatto.
            """;

    private final AiConfig ai;
    private final FoodService foods;
    private final ShoppingService shopping;
    private final ObjectMapper mapper;

    public KitchenAssistantAgent(AiConfig ai, FoodService foods, ShoppingService shopping, ObjectMapper mapper) {
        this.ai = ai;
        this.foods = foods;
        this.shopping = shopping;
        this.mapper = mapper;
    }

    public ChatResult chat(Long userId, List<ChatTurn> history, String userMessage) {
        var client = ai.client();
        List<MessageParam> messages = new ArrayList<>();
        // Lo storico arriva dal browser solo come testo: i turni precedenti non contengono tool call.
        for (ChatTurn turn : history) {
            if (turn.text() == null || turn.text().isBlank()) continue;
            messages.add("assistant".equals(turn.role())
                    ? MessageParam.ofAssistant(turn.text())
                    : MessageParam.ofUser(turn.text()));
        }
        messages.add(MessageParam.ofUser(userMessage));

        List<String> actions = new ArrayList<>();
        String system = SYSTEM + "\nOggi è " + LocalDate.now() + ".";

        for (int step = 0; step < MAX_STEPS; step++) {
            MessageCreateParams.Builder builder = MessageCreateParams.builder()
                    .model(ai.model())
                    .maxTokens(16000L)
                    .system(system)
                    .tools(TOOLS.stream().map(com.anthropic.models.messages.ToolUnion::ofTool).toList())
                    .messages(messages);
            ai.applyFallback(builder);
            MessageCreateParams params = builder.build();

            Message response = client.messages().create(params);
            StopReason stop = response.stopReason().orElse(StopReason.END_TURN);

            if (StopReason.REFUSAL.equals(stop)) {
                return new ChatResult("Non posso aiutarti con questa richiesta.", actions);
            }
            // Il turno dell'assistente va rimandato intero (compresi i blocchi di thinking), senza modifiche.
            messages.add(response.toParam());

            List<ToolUseBlock> toolCalls = response.content().stream()
                    .flatMap(b -> b.toolUse().stream())
                    .toList();
            if (!StopReason.TOOL_USE.equals(stop) || toolCalls.isEmpty()) {
                return new ChatResult(text(response), actions);
            }

            List<ContentBlockParam> results = new ArrayList<>();
            for (ToolUseBlock call : toolCalls) {
                results.add(ContentBlockParam.ofToolResult(execute(userId, call, actions)));
            }
            // Tutti i risultati in un unico messaggio utente.
            messages.add(MessageParam.ofUserOfContentBlockParams(results));
        }
        return new ChatResult("Mi sono fermato perché la richiesta richiedeva troppi passaggi. Puoi riformularla?", actions);
    }

    private ToolResultBlockParam execute(Long userId, ToolUseBlock call, List<String> actions) {
        try {
            JsonNode input = mapper.readTree(mapper.writeValueAsString(call._input()));
            Object result = switch (call.name()) {
                case "list_fridge_items" -> foods.list(userId).stream().map(KitchenAssistantAgent::summary).toList();
                case "add_fridge_item" -> {
                    FoodResponse f = foods.create(userId, new FoodRequest(
                            input.path("name").asText(),
                            FoodCategory.parse(input.path("category").asText(null)),
                            input.hasNonNull("quantity") ? input.get("quantity").asDouble() : null,
                            textOrNull(input, "unit"),
                            input.hasNonNull("expiry_date") && !input.get("expiry_date").asText().isBlank()
                                    ? LocalDate.parse(input.get("expiry_date").asText()) : null,
                            FoodSource.ASSISTANT,
                            null));
                    actions.add("Aggiunto al frigo: " + f.name());
                    yield summary(f);
                }
                case "remove_fridge_item" -> {
                    long id = input.path("id").asLong();
                    boolean toShopping = input.path("add_to_shopping_list").asBoolean(false);
                    String name = foods.list(userId).stream().filter(f -> f.id() == id)
                            .map(FoodResponse::name).findFirst().orElse("#" + id);
                    foods.remove(userId, id, toShopping);
                    actions.add("Rimosso dal frigo: " + name + (toShopping ? " (aggiunto alla spesa)" : ""));
                    yield Map.of("removed", id, "added_to_shopping_list", toShopping);
                }
                case "list_shopping_list" -> shopping.list(userId);
                case "add_to_shopping_list" -> {
                    List<String> added = new ArrayList<>();
                    for (JsonNode item : input.path("items")) {
                        var r = shopping.add(userId, item.path("name").asText(), textOrNull(item, "quantity"));
                        if (r != null) added.add(r.name());
                    }
                    if (!added.isEmpty()) actions.add("Aggiunto alla spesa: " + String.join(", ", added));
                    yield Map.of("added", added);
                }
                default -> throw new IllegalArgumentException("Strumento sconosciuto: " + call.name());
            };
            return ToolResultBlockParam.builder()
                    .toolUseId(call.id())
                    .content(mapper.writeValueAsString(result))
                    .build();
        } catch (Exception e) {
            log.warn("Errore nello strumento {}: {}", call.name(), e.getMessage());
            return ToolResultBlockParam.builder()
                    .toolUseId(call.id())
                    .content("Errore: " + e.getMessage())
                    .isError(true)
                    .build();
        }
    }

    private static Map<String, Object> summary(FoodResponse f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.id());
        m.put("name", f.name());
        m.put("category", f.category());
        m.put("quantity", f.quantity());
        m.put("unit", f.unit());
        m.put("expiry_date", f.expiryDate());
        m.put("days_to_expiry", f.daysToExpiry());
        return m;
    }

    private static String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) && !node.get(field).asText().isBlank() ? node.get(field).asText() : null;
    }

    private static String text(Message response) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(t -> sb.append(t.text()));
        }
        return sb.toString().trim();
    }

    // ---- Definizione degli strumenti ----

    private static final List<Tool> TOOLS = List.of(
            tool("list_fridge_items",
                    "Elenca gli alimenti nel frigo dell'utente, ordinati per scadenza, con id, quantità e giorni alla scadenza.",
                    Map.of(), List.of()),
            tool("add_fridge_item",
                    "Aggiunge un alimento al frigo dell'utente.",
                    Map.of(
                            "name", Map.of("type", "string", "description", "Nome dell'alimento"),
                            "category", Map.of("type", "string", "enum", List.of(
                                    "DAIRY", "MEAT", "FISH", "VEGETABLES", "FRUIT", "EGGS", "BEVERAGES",
                                    "SAUCES", "LEFTOVERS", "BAKERY", "FROZEN", "OTHER")),
                            "quantity", Map.of("type", "number"),
                            "unit", Map.of("type", "string", "description", "pz, g, kg, ml, l, conf"),
                            "expiry_date", Map.of("type", "string", "description", "Data ISO yyyy-MM-dd, se nota")),
                    List.of("name", "category")),
            tool("remove_fridge_item",
                    "Rimuove un alimento dal frigo (consumato o buttato). Usa l'id ottenuto da list_fridge_items.",
                    Map.of(
                            "id", Map.of("type", "integer"),
                            "add_to_shopping_list", Map.of("type", "boolean",
                                    "description", "true per rimetterlo nella lista della spesa")),
                    List.of("id", "add_to_shopping_list")),
            tool("list_shopping_list",
                    "Mostra la lista della spesa dell'utente.",
                    Map.of(), List.of()),
            tool("add_to_shopping_list",
                    "Aggiunge uno o più articoli alla lista della spesa. I duplicati già presenti vengono ignorati.",
                    Map.of("items", Map.of("type", "array", "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "name", Map.of("type", "string"),
                                    "quantity", Map.of("type", "string", "description", "Es. '1 l', '6 pz'")),
                            "required", List.of("name")))),
                    List.of("items")));

    private static Tool tool(String name, String description, Map<String, Object> properties, List<String> required) {
        Tool.InputSchema.Properties.Builder props = Tool.InputSchema.Properties.builder();
        properties.forEach((k, v) -> props.putAdditionalProperty(k, JsonValue.from(v)));
        return Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(Tool.InputSchema.builder()
                        .properties(props.build())
                        .required(required)
                        .build())
                .build();
    }
}
