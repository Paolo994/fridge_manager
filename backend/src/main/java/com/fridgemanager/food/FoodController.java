package com.fridgemanager.food;

import com.fridgemanager.food.FoodDtos.FoodRequest;
import com.fridgemanager.food.FoodDtos.FoodResponse;
import com.fridgemanager.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    private final FoodService service;

    public FoodController(FoodService service) {
        this.service = service;
    }

    @GetMapping
    public List<FoodResponse> list(@AuthenticationPrincipal AuthUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody FoodRequest req) {
        return service.create(user.id(), req);
    }

    /** Salva in un colpo solo gli alimenti confermati dopo il riconoscimento da foto. */
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<FoodResponse> createAll(@AuthenticationPrincipal AuthUser user,
                                        @Valid @RequestBody List<@Valid FoodRequest> reqs) {
        return service.createAll(user.id(), reqs);
    }

    @PutMapping("/{id}")
    public FoodResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                               @Valid @RequestBody FoodRequest req) {
        return service.update(user.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                       @RequestParam(defaultValue = "false") boolean addToShopping) {
        service.remove(user.id(), id, addToShopping);
    }
}
