package com.fridgemanager.security;

import com.fridgemanager.user.User;
import com.fridgemanager.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  @NotBlank @Size(max = 80) String displayName) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserDto(Long id, String email, String displayName) {
        static UserDto of(User u) { return new UserDto(u.getId(), u.getEmail(), u.getDisplayName()); }
    }

    public record AuthResponse(String token, UserDto user) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esiste già un account con questa email");
        }
        User user = users.save(new User(email, encoder.encode(req.password()), req.displayName().trim()));
        return new AuthResponse(jwt.issue(user), UserDto.of(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o password non validi"));
        return new AuthResponse(jwt.issue(user), UserDto.of(user));
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthUser auth) {
        return users.findById(auth.id()).map(UserDto::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
