package com.sft.category;

import com.sft.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    /** Nombre de la categoría de sistema que se usa cuando nada encaja. */
    public static final String DEFAULT_NAME = "Otros";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 7)
    private String color;

    @Column(length = 40)
    private String icon;

    /** NULL = categoría del sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_system", nullable = false)
    private boolean system;
}
