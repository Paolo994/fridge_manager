package com.fridgemanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fridgemanager.notification.ExpiryNotificationJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "app.ai.api-key="
})
@AutoConfigureMockMvc
class ApiFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ExpiryNotificationJob job;

    @Test
    void fullFlow() throws Exception {
        // Senza token le API sono protette
        mvc.perform(get("/api/food")).andExpect(status().isUnauthorized());

        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"Mario@Example.com","password":"password123","displayName":"Mario"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + json.readTree(body).get("token").asText();

        // Email duplicata (anche con maiuscole diverse)
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"mario@example.com","password":"password123","displayName":"Mario"}"""))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"mario@example.com","password":"sbagliata"}"""))
                .andExpect(status().isUnauthorized());

        String tomorrow = LocalDate.now().plusDays(1).toString();
        String created = mvc.perform(post("/api/food").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Latte","category":"DAIRY","quantity":1,"unit":"l","expiryDate":"%s"}""".formatted(tomorrow)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXPIRING_SOON"))
                .andReturn().getResponse().getContentAsString();
        long foodId = json.readTree(created).get("id").asLong();

        // L'altro utente non vede né modifica il cibo di Mario
        String other = json.readTree(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"luigi@example.com","password":"password123","displayName":"Luigi"}"""))
                .andReturn().getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/food").header("Authorization", "Bearer " + other))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/food/" + foodId).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());

        // Il job crea una sola notifica anche se gira due volte
        job.run();
        job.run();
        mvc.perform(get("/api/notifications").header("Authorization", token))
                .andExpect(jsonPath("$.unread").value(1))
                .andExpect(jsonPath("$.items[0].message").value("Latte scade domani"));

        // Consumato -> rimesso nella lista della spesa
        mvc.perform(delete("/api/food/" + foodId + "?addToShopping=true").header("Authorization", token))
                .andExpect(status().isNoContent());
        JsonNode shopping = json.readTree(mvc.perform(get("/api/shopping").header("Authorization", token))
                .andReturn().getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(shopping.get(0).get("name").asText()).isEqualTo("Latte");

        // Senza chiave API le funzioni IA rispondono 503
        mvc.perform(get("/api/ai/status").header("Authorization", token))
                .andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(post("/api/ai/recipes").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable());
    }
}
