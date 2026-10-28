package com.sft.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Categorías visibles para un usuario: las del sistema + las suyas. */
    @Query("""
            select c from Category c
            where c.system = true or c.user.id = :userId
            order by c.system desc, lower(c.name)
            """)
    List<Category> findVisible(@Param("userId") Long userId);

    @Query("""
            select c from Category c
            where c.id = :id and (c.system = true or c.user.id = :userId)
            """)
    Optional<Category> findVisibleById(@Param("id") Long id, @Param("userId") Long userId);

    /** Por nombre (sin distinguir mayúsculas). Primero las del usuario, luego las del sistema. */
    @Query("""
            select c from Category c
            where lower(c.name) = lower(:name) and (c.system = true or c.user.id = :userId)
            order by c.system asc
            """)
    List<Category> findVisibleByName(@Param("name") String name, @Param("userId") Long userId);

    @Query("select c from Category c where c.system = true and lower(c.name) = lower(:name)")
    Optional<Category> findSystemByName(@Param("name") String name);

    @Query("""
            select count(c) from Category c
            where lower(c.name) = lower(:name) and (c.system = true or c.user.id = :userId)
            """)
    long countVisibleByName(@Param("name") String name, @Param("userId") Long userId);
}
