package com.sft.transaction;

import com.sft.auth.AuthenticatedUser;
import com.sft.transaction.dto.TransactionRequest;
import com.sft.transaction.dto.TransactionResponse;
import com.sft.transaction.dto.TransactionUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId) {
        return transactionService.list(user.id(), from, to, categoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.create(user.id(), request);
    }

    @PatchMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable Long id,
                                      @Valid @RequestBody TransactionUpdateRequest request) {
        return transactionService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        transactionService.delete(user.id(), id);
    }

    @PostMapping("/{id}/recategorize")
    public TransactionResponse recategorize(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable Long id) {
        return transactionService.recategorize(user.id(), id);
    }
}
