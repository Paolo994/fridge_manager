package com.fridgemanager.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.fridgemanager.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Punto unico di accesso a Claude. Il client esiste solo se è configurata la chiave API:
 * senza chiave le funzioni IA rispondono 503 e il resto dell'app funziona normalmente.
 */
@Component
public class AiConfig {

    private final AnthropicClient client;
    private final String model;

    public AiConfig(AppProperties props) {
        this.client = props.ai().enabled()
                ? AnthropicOkHttpClient.builder().apiKey(props.ai().apiKey()).build()
                : null;
        this.model = props.ai().model();
    }

    public boolean enabled() {
        return client != null;
    }

    public AnthropicClient client() {
        if (client == null) {
            throw new AiUnavailableException("Funzioni IA non configurate: imposta ANTHROPIC_API_KEY sul backend");
        }
        return client;
    }

    public String model() {
        return model;
    }

    /**
     * Fallback lato server in caso di rifiuto dei filtri di sicurezza: il server riprova su un modello di riserva.
     * È supportato solo da alcuni modelli (non da Haiku), quindi lo aggiungiamo solo quando serve.
     */
    public void applyFallback(com.anthropic.models.messages.MessageCreateParams.Builder builder) {
        if (model.startsWith("claude-opus-5") || model.startsWith("claude-fable-5-1") || model.equals("claude-sonnet-5-5")) {
            builder.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                   .putAdditionalBodyProperty("fallbacks", com.anthropic.core.JsonValue.from("default"));
        }
    }
}
