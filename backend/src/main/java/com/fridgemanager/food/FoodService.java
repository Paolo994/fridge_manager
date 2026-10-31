package com.fridgemanager.food;

import com.fridgemanager.food.FoodDtos.FoodRequest;
import com.fridgemanager.food.FoodDtos.FoodResponse;
import com.fridgemanager.shopping.ShoppingService;
import com.fridgemanager.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class FoodService {

    private final FoodItemRepository foods;
    private final UserRepository users;
    private final ShoppingService shopping;

    public FoodService(FoodItemRepository foods, UserRepository users, ShoppingService shopping) {
        this.foods = foods;
        this.users = users;
        this.shopping = shopping;
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> list(Long userId) {
        return foods.findAllForOwner(userId).stream().map(FoodResponse::of).toList();
    }

    public FoodResponse create(Long userId, FoodRequest req) {
        FoodItem item = new FoodItem(users.getReferenceById(userId));
        apply(item, req);
        return FoodResponse.of(foods.save(item));
    }

    public List<FoodResponse> createAll(Long userId, List<FoodRequest> reqs) {
        return reqs.stream().map(r -> create(userId, r)).toList();
    }

    public FoodResponse update(Long userId, Long id, FoodRequest req) {
        FoodItem item = find(userId, id);
        apply(item, req);
        return FoodResponse.of(item);
    }

    /** Rimuove un alimento (consumato o buttato); se richiesto lo rimette nella lista della spesa. */
    public void remove(Long userId, Long id, boolean addToShopping) {
        FoodItem item = find(userId, id);
        if (addToShopping) {
            shopping.add(userId, item.getName(), null);
        }
        foods.delete(item);
    }

    private FoodItem find(Long userId, Long id) {
        return foods.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alimento non trovato"));
    }

    private static void apply(FoodItem item, FoodRequest req) {
        item.setName(req.name().trim());
        item.setCategory(req.category() == null ? FoodCategory.OTHER : req.category());
        item.setQuantity(req.quantity());
        item.setUnit(req.unit());
        item.setExpiryDate(req.expiryDate());
        if (req.source() != null) item.setSource(req.source());
        item.setNotes(req.notes());
    }
}
