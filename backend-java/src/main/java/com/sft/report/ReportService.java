package com.sft.report;

import com.sft.report.dto.CategoryTotal;
import com.sft.report.dto.MonthlyReportItem;
import com.sft.report.dto.MonthlyTotalRow;
import com.sft.transaction.TransactionRepository;
import com.sft.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private static final String UNCATEGORIZED = "Sin categoría";

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Siempre devuelve los 12 meses (con ceros), listos para pintar en un gráfico. */
    @Transactional(readOnly = true)
    public List<MonthlyReportItem> monthly(Long userId, Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        BigDecimal[] income = zeros();
        BigDecimal[] expense = zeros();

        for (MonthlyTotalRow row : transactionRepository.totalsByMonth(userId, y)) {
            int idx = row.month() - 1;
            if (row.type() == TransactionType.INCOME) {
                income[idx] = row.total();
            } else {
                expense[idx] = row.total();
            }
        }
        List<MonthlyReportItem> result = new ArrayList<>(12);
        for (int m = 0; m < 12; m++) {
            result.add(new MonthlyReportItem(m + 1, income[m], expense[m]));
        }
        return result;
    }

    /** Totales por categoría en un rango (por defecto: mes actual) y tipo (por defecto: gastos). */
    @Transactional(readOnly = true)
    public List<CategoryTotal> byCategory(Long userId, LocalDate from, LocalDate to, TransactionType type) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : start.plusMonths(1).minusDays(1);
        TransactionType t = type != null ? type : TransactionType.EXPENSE;

        return transactionRepository.totalsByCategory(userId, t, start, end).stream()
                .map(r -> r.name() == null ? new CategoryTotal(null, UNCATEGORIZED, "#868E96", r.total()) : r)
                .toList();
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] arr = new BigDecimal[12];
        java.util.Arrays.fill(arr, BigDecimal.ZERO);
        return arr;
    }
}
