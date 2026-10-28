package com.sft.report;

import com.sft.auth.AuthenticatedUser;
import com.sft.report.dto.CategoryTotal;
import com.sft.report.dto.MonthlyReportItem;
import com.sft.transaction.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    public List<MonthlyReportItem> monthly(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam(required = false) Integer year) {
        return reportService.monthly(user.id(), year);
    }

    @GetMapping("/by-category")
    public List<CategoryTotal> byCategory(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type) {
        return reportService.byCategory(user.id(), from, to, type);
    }
}
