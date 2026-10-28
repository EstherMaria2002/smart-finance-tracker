package com.sft.category;

import com.sft.auth.AuthenticatedUser;
import com.sft.category.dto.CategoryRequest;
import com.sft.category.dto.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return categoryService.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody CategoryRequest request) {
        return categoryService.create(user.id(), request);
    }
}
