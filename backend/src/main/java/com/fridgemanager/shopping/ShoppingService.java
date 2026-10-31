package com.fridgemanager.shopping;

import com.fridgemanager.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ShoppingService {

    public record ShoppingResponse(Long id, String name, String quantity, boolean checked) {
        static ShoppingResponse of(ShoppingItem s) {
            return new ShoppingResponse(s.getId(), s.getName(), s.getQuantity(), s.isChecked());
        }
    }

    private final ShoppingItemRepository items;
    private final UserRepository users;

    public ShoppingService(ShoppingItemRepository items, UserRepository users) {
        this.items = items;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ShoppingResponse> list(Long userId) {
        return items.findByOwnerIdOrderByCheckedAscCreatedAtDesc(userId).stream().map(ShoppingResponse::of).toList();
    }

    /** Aggiunge un articolo, ignorando i duplicati ancora da comprare. Restituisce null se era già presente. */
    public ShoppingResponse add(Long userId, String name, String quantity) {
        String clean = name.trim();
        if (items.existsByOwnerIdAndNameIgnoreCaseAndCheckedFalse(userId, clean)) {
            return null;
        }
        return ShoppingResponse.of(items.save(new ShoppingItem(users.getReferenceById(userId), clean, quantity)));
    }

    public ShoppingResponse update(Long userId, Long id, String name, String quantity, Boolean checked) {
        ShoppingItem item = find(userId, id);
        if (name != null && !name.isBlank()) item.setName(name.trim());
        if (quantity != null) item.setQuantity(quantity);
        if (checked != null) item.setChecked(checked);
        return ShoppingResponse.of(item);
    }

    public void delete(Long userId, Long id) {
        items.delete(find(userId, id));
    }

    public void clearChecked(Long userId) {
        items.deleteByOwnerIdAndCheckedTrue(userId);
    }

    private ShoppingItem find(Long userId, Long id) {
        return items.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Articolo non trovato"));
    }
}
