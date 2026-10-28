package com.sft.budget;

import com.sft.auth.AuthenticatedUser;
import com.sft.budget.dto.BudgetRequest;
import com.sft.budget.dto.BudgetResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /** ?month=2026-09-01 (cualquier día del mes sirve). Sin parámetro: mes actual. */
    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return budgetService.list(user.id(), month);
    }

    @PutMapping
    public BudgetResponse upsert(@AuthenticationPrincipal AuthenticatedUser user,
                                 @Valid @RequestBody BudgetRequest request) {
        return budgetService.upsert(user.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        budgetService.delete(user.id(), id);
    }
}
