package com.sft.category.dto;

import com.sft.category.Category;

public record CategoryResponse(Long id, String name, String color, String icon, boolean system) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getColor(), c.getIcon(), c.isSystem());
    }
}
