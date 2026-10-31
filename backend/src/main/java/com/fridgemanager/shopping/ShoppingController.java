package com.fridgemanager.shopping;

import com.fridgemanager.security.AuthUser;
import com.fridgemanager.shopping.ShoppingService.ShoppingResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/shopping")
public class ShoppingController {

    public record AddRequest(@NotBlank @Size(max = 120) String name, @Size(max = 40) String quantity) {}

    public record AddManyRequest(@NotNull List<@Valid AddRequest> items) {}

    public record UpdateRequest(@Size(max = 120) String name, @Size(max = 40) String quantity, Boolean checked) {}

    private final ShoppingService service;

    public ShoppingController(ShoppingService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShoppingResponse> list(@AuthenticationPrincipal AuthUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ShoppingResponse> add(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AddManyRequest req) {
        return req.items().stream()
                .map(i -> service.add(user.id(), i.name(), i.quantity()))
                .filter(Objects::nonNull)
                .toList();
    }

    @PatchMapping("/{id}")
    public ShoppingResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                   @Valid @RequestBody UpdateRequest req) {
        return service.update(user.id(), id, req.name(), req.quantity(), req.checked());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
    }

    @DeleteMapping("/checked")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearChecked(@AuthenticationPrincipal AuthUser user) {
        service.clearChecked(user.id());
    }
}
