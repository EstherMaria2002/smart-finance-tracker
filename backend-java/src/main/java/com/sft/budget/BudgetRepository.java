package com.sft.budget;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @EntityGraph(attributePaths = "category")
    List<Budget> findByUser_IdAndMonthOrderByIdAsc(Long userId, LocalDate month);

    @EntityGraph(attributePaths = "category")
    Optional<Budget> findByUser_IdAndCategory_IdAndMonth(Long userId, Long categoryId, LocalDate month);

    Optional<Budget> findByIdAndUser_Id(Long id, Long userId);
}
