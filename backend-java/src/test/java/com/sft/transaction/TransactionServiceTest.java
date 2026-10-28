package com.sft.transaction;

import com.sft.category.Category;
import com.sft.category.CategoryService;
import com.sft.common.exception.MlServiceException;
import com.sft.config.MlProperties;
import com.sft.ml.MlCategoryResponse;
import com.sft.ml.MlClient;
import com.sft.ml.MlFallbackService;
import com.sft.transaction.dto.TransactionRequest;
import com.sft.transaction.dto.TransactionResponse;
import com.sft.user.User;
import com.sft.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long USER_ID = 1L;

    @Mock TransactionRepository transactionRepository;
    @Mock UserRepository userRepository;
    @Mock CategoryService categoryService;
    @Mock MlClient mlClient;
    @Mock TransactionTemplate txTemplate;

    private TransactionService service;

    private final Category restaurantes = category(7L, "Restaurantes");
    private final Category otros = category(12L, "Otros");

    @BeforeEach
    void setUp() {
        // El fallback es real (son reglas puras); el resto, mocks.
        service = new TransactionService(transactionRepository, userRepository, categoryService, mlClient,
                new MlFallbackService(), new MlProperties("http://ml", "key", 800, 0.30), txTemplate);

        when(userRepository.getReferenceById(anyLong())).thenReturn(new User());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void manualCategory_doesNotCallMl() {
        when(categoryService.getVisible(USER_ID, 7L)).thenReturn(restaurantes);

        TransactionResponse res = service.create(USER_ID, request("Cena", 7L));

        assertThat(res.categorySource()).isEqualTo(CategorySource.MANUAL);
        assertThat(res.mlConfidence()).isNull();
        verify(mlClient, never()).predict(anyString(), any());
    }

    @Test
    void nullCategory_usesMlPrediction_andStoresConfidence() {
        when(mlClient.predict(anyString(), any())).thenReturn(new MlCategoryResponse("Restaurantes", 0.87, "ML"));
        when(categoryService.findVisibleByName(USER_ID, "Restaurantes")).thenReturn(Optional.of(restaurantes));

        TransactionResponse res = service.create(USER_ID, request("Cena en restaurante italiano", null));

        assertThat(res.category().name()).isEqualTo("Restaurantes");
        assertThat(res.categorySource()).isEqualTo(CategorySource.ML);
        assertThat(res.mlConfidence()).isEqualByComparingTo("0.870");
    }

    @Test
    void mlDown_fallsBackToRules_andSourceIsRule() {
        // Timeout, caída o circuito abierto: el servicio NO debe propagar la excepción.
        when(mlClient.predict(anyString(), any())).thenThrow(new MlServiceException("timeout"));
        when(categoryService.findVisibleByName(USER_ID, "Restaurantes")).thenReturn(Optional.of(restaurantes));

        TransactionResponse res = service.create(USER_ID, request("Cena en restaurante italiano", null));

        assertThat(res.category().name()).isEqualTo("Restaurantes");
        assertThat(res.categorySource()).isEqualTo(CategorySource.RULE);
        assertThat(res.mlConfidence()).isNull();
    }

    @Test
    void mlDown_andNoKeywordMatch_usesOtros() {
        when(mlClient.predict(anyString(), any())).thenThrow(new MlServiceException("conexión rechazada"));
        when(categoryService.findVisibleByName(USER_ID, "Otros")).thenReturn(Optional.of(otros));

        TransactionResponse res = service.create(USER_ID, request("Pago varios", null));

        assertThat(res.category().name()).isEqualTo("Otros");
        assertThat(res.categorySource()).isEqualTo(CategorySource.RULE);
    }

    @Test
    void mlReturnsUnknownCategory_isMappedToOtros() {
        when(mlClient.predict(anyString(), any())).thenReturn(new MlCategoryResponse("CategoriaInventada", 0.95, "ML"));
        when(categoryService.findVisibleByName(USER_ID, "CategoriaInventada")).thenReturn(Optional.empty());
        when(categoryService.findVisibleByName(USER_ID, "Otros")).thenReturn(Optional.of(otros));

        TransactionResponse res = service.create(USER_ID, request("Pago varios", null));

        assertThat(res.category().name()).isEqualTo("Otros");
        assertThat(res.categorySource()).isEqualTo(CategorySource.RULE);
    }

    @Test
    void mlWithoutModel_isIgnored_andRulesAreUsed() {
        // El ML sin modelo responde { Otros, 0.0, RULE } con 200: no es una predicción real.
        when(mlClient.predict(anyString(), any())).thenReturn(new MlCategoryResponse("Otros", 0.0, "RULE"));
        Category transporte = category(3L, "Transporte");
        when(categoryService.findVisibleByName(USER_ID, "Transporte")).thenReturn(Optional.of(transporte));

        TransactionResponse res = service.create(USER_ID, request("Uber al aeropuerto", null));

        assertThat(res.category().name()).isEqualTo("Transporte");
        assertThat(res.categorySource()).isEqualTo(CategorySource.RULE);
    }

    @Test
    void lowConfidenceMlPrediction_isIgnored() {
        when(mlClient.predict(anyString(), any())).thenReturn(new MlCategoryResponse("Restaurantes", 0.10, "ML"));
        when(categoryService.findVisibleByName(USER_ID, "Otros")).thenReturn(Optional.of(otros));

        TransactionResponse res = service.create(USER_ID, request("Pago varios", null));

        assertThat(res.categorySource()).isEqualTo(CategorySource.RULE);
        assertThat(res.category().name()).isEqualTo("Otros");
    }

    // ---- helpers
    private static TransactionRequest request(String description, Long categoryId) {
        return new TransactionRequest(new BigDecimal("42.50"), TransactionType.EXPENSE, description,
                LocalDate.of(2026, 9, 20), categoryId);
    }

    private static Category category(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setSystem(true);
        return c;
    }
}
