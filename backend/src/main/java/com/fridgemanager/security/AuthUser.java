package com.fridgemanager.security;

/** Utente autenticato, ricavato dal token JWT. */
public record AuthUser(Long id, String email) {}
