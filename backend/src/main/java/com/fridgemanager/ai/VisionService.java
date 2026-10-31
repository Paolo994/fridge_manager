package com.fridgemanager.ai;

import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.fridgemanager.ai.AiModels.FoodRecognition;
import com.fridgemanager.ai.AiModels.RecognizedFood;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Riconoscimento del cibo dalle foto. È una singola chiamata con output strutturato:
 * non serve un agente perché il compito è ben definito (immagine in ingresso, lista in uscita).
 * I risultati non vengono salvati qui: l'utente li rivede e conferma dalla webapp.
 */
@Service
public class VisionService {

    private static final Set<String> SUPPORTED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private static final String SINGLE_ITEM_PROMPT = """
            Nella foto c'è uno o pochi alimenti, spesso con l'etichetta in vista.
            Riconosci ogni alimento e leggi la data di scadenza stampata ("da consumarsi entro",
            "da consumarsi preferibilmente entro", "scad.", "exp", "best before").
            Se la data mostra solo mese e anno, usa l'ultimo giorno del mese.
            Se la data non è leggibile, lascia expiryDate vuoto e expiryFromLabel=false: non inventarla.
            Oggi è %s.
            """;

    private static final String FRIDGE_PROMPT = """
            La foto mostra l'interno di un frigorifero. Elenca gli alimenti che riesci a identificare
            con ragionevole sicurezza, un elemento per prodotto (raggruppa confezioni identiche nella quantità).
            Ignora contenitori opachi di cui non vedi il contenuto, a meno che l'etichetta lo dica.
            Se leggi una data di scadenza usala (expiryFromLabel=true). Altrimenti, per gli alimenti freschi
            stima una scadenza prudente a partire da oggi (expiryFromLabel=false); per gli altri lascia expiryDate vuoto.
            Oggi è %s.
            """;

    private final AiConfig ai;

    public VisionService(AiConfig ai) {
        this.ai = ai;
    }

    public List<RecognizedFood> recognizeFood(MultipartFile photo) {
        return analyze(photo, SINGLE_ITEM_PROMPT.formatted(LocalDate.now()));
    }

    public List<RecognizedFood> scanFridge(MultipartFile photo) {
        return analyze(photo, FRIDGE_PROMPT.formatted(LocalDate.now()));
    }

    private List<RecognizedFood> analyze(MultipartFile photo, String prompt) {
        String mime = photo.getContentType();
        if (mime == null || !SUPPORTED.contains(mime)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formati supportati: JPEG, PNG, WEBP, GIF");
        }
        String base64;
        try {
            base64 = Base64.getEncoder().encodeToString(photo.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossibile leggere la foto");
        }

        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(ai.model())
                .maxTokens(16000L);
        ai.applyFallback(builder);
        StructuredMessageCreateParams<FoodRecognition> params = builder
                .addUserMessageOfContentBlockParams(List.of(
                        ContentBlockParam.ofImage(ImageBlockParam.builder()
                                .source(Base64ImageSource.builder()
                                        .mediaType(Base64ImageSource.MediaType.of(mime))
                                        .data(base64)
                                        .build())
                                .build()),
                        ContentBlockParam.ofText(TextBlockParam.builder().text(prompt).build())))
                .outputConfig(FoodRecognition.class)
                .build();

        StructuredMessage<FoodRecognition> response = ai.client().messages().create(params);
        if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "L'IA non ha potuto analizzare questa immagine");
        }
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .findFirst()
                .map(typed -> typed.text().items())
                .orElse(List.of());
    }
}
