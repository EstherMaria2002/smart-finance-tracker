package com.sft.transaction;

import com.sft.report.dto.CategoryTotal;
import com.sft.report.dto.MonthlyTotalRow;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    // Se redeclara solo para añadir el EntityGraph: carga la categoría en la misma consulta (evita N+1
    // y LazyInitializationException, ya que open-in-view está desactivado).
    @Override
    @EntityGraph(attributePaths = "category")
    List<Transaction> findAll(Specification<Transaction> spec, Sort sort);

    @EntityGraph(attributePaths = "category")
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select new com.sft.report.dto.MonthlyTotalRow(month(t.occurredOn), t.type, sum(t.amount))
            from Transaction t
            where t.user.id = :userId and year(t.occurredOn) = :year
            group by month(t.occurredOn), t.type
            """)
    List<MonthlyTotalRow> totalsByMonth(@Param("userId") Long userId, @Param("year") int year);

    @Query("""
            select new com.sft.report.dto.CategoryTotal(c.id, c.name, c.color, sum(t.amount))
            from Transaction t left join t.category c
            where t.user.id = :userId and t.type = :type
              and t.occurredOn >= :from and t.occurredOn <= :to
            group by c.id, c.name, c.color
            order by sum(t.amount) desc
            """)
    List<CategoryTotal> totalsByCategory(@Param("userId") Long userId,
                                         @Param("type") TransactionType type,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}
