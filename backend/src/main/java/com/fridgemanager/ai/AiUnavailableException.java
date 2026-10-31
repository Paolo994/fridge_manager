package com.fridgemanager.ai;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AiUnavailableException extends ResponseStatusException {
    public AiUnavailableException(String reason) {
        super(HttpStatus.SERVICE_UNAVAILABLE, reason);
    }
}
