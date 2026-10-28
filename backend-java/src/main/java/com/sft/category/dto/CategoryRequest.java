package com.sft.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener formato #RRGGBB") String color,
        @Size(max = 40) String icon
) {
}
