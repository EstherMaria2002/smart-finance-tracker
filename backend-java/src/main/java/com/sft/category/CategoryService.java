package com.sft.category;

import com.sft.category.dto.CategoryRequest;
import com.sft.category.dto.CategoryResponse;
import com.sft.common.exception.ConflictException;
import com.sft.common.exception.ResourceNotFoundException;
import com.sft.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categoryRepository.findVisible(userId).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest req) {
        String name = req.name().trim();
        // No se permite duplicar ni una categoría propia ni una del sistema.
        if (categoryRepository.countVisibleByName(name, userId) > 0) {
            throw new ConflictException("Ya existe una categoría llamada '" + name + "'");
        }
        Category category = new Category();
        category.setName(name);
        category.setColor(req.color());
        category.setIcon(req.icon());
        category.setSystem(false);
        category.setUser(userRepository.getReferenceById(userId));
        return CategoryResponse.from(categoryRepository.save(category));
    }

    /** Categoría que el usuario puede usar (suya o del sistema); 404 si no existe o es de otro usuario. */
    @Transactional(readOnly = true)
    public Category getVisible(Long userId, Long categoryId) {
        return categoryRepository.findVisibleById(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    @Transactional(readOnly = true)
    public Optional<Category> findVisibleByName(Long userId, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return categoryRepository.findVisibleByName(name.trim(), userId).stream().findFirst();
    }

    /** La categoría de sistema "Otros" (la crea la migración V2). */
    @Transactional(readOnly = true)
    public Category getDefault() {
        return categoryRepository.findSystemByName(Category.DEFAULT_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta la categoría de sistema '" + Category.DEFAULT_NAME + "' (¿migración V2?)"));
    }
}
