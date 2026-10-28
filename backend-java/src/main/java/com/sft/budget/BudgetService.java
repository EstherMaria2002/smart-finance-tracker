package com.sft.budget;

import com.sft.budget.dto.BudgetRequest;
import com.sft.budget.dto.BudgetResponse;
import com.sft.category.Category;
import com.sft.category.CategoryService;
import com.sft.common.exception.ResourceNotFoundException;
import com.sft.report.dto.CategoryTotal;
import com.sft.transaction.TransactionRepository;
import com.sft.transaction.TransactionType;
import com.sft.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         CategoryService categoryService,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId, LocalDate month) {
        LocalDate first = firstDayOf(month);
        Map<Long, BigDecimal> spentByCategory = spentByCategory(userId, first);
        return budgetRepository.findByUser_IdAndMonthOrderByIdAsc(userId, first).stream()
                .map(b -> BudgetResponse.from(b, spentByCategory.getOrDefault(b.getCategory().getId(), BigDecimal.ZERO)))
                .toList();
    }

    /** PUT /api/budgets: crea el presupuesto (usuario, categoría, mes) o actualiza su límite. */
    @Transactional
    public BudgetResponse upsert(Long userId, BudgetRequest req) {
        LocalDate first = firstDayOf(req.month());
        Category category = categoryService.getVisible(userId, req.categoryId());

        Budget budget = budgetRepository.findByUser_IdAndCategory_IdAndMonth(userId, category.getId(), first)
                .orElseGet(() -> {
                    Budget b = new Budget();
                    b.setUser(userRepository.getReferenceById(userId));
                    b.setCategory(category);
                    b.setMonth(first);
                    return b;
                });
        budget.setLimitAmount(req.limitAmount());
        Budget saved = budgetRepository.save(budget);

        BigDecimal spent = spentByCategory(userId, first).getOrDefault(category.getId(), BigDecimal.ZERO);
        return BudgetResponse.from(saved, spent);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Budget budget = budgetRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Presupuesto no encontrado"));
        budgetRepository.delete(budget);
    }

    private Map<Long, BigDecimal> spentByCategory(Long userId, LocalDate firstDay) {
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);
        Map<Long, BigDecimal> result = new HashMap<>();
        for (CategoryTotal row : transactionRepository.totalsByCategory(userId, TransactionType.EXPENSE, firstDay, lastDay)) {
            if (row.categoryId() != null) {
                result.put(row.categoryId(), row.total());
            }
        }
        return result;
    }

    private static LocalDate firstDayOf(LocalDate date) {
        return (date == null ? LocalDate.now() : date).withDayOfMonth(1);
    }
}
