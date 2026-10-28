package com.sft.transaction;

import com.sft.category.Category;
import com.sft.category.CategoryService;
import com.sft.common.exception.MlServiceException;
import com.sft.common.exception.ResourceNotFoundException;
import com.sft.config.MlProperties;
import com.sft.ml.MlCategoryResponse;
import com.sft.ml.MlClient;
import com.sft.ml.MlFallbackService;
import com.sft.transaction.dto.TransactionRequest;
import com.sft.transaction.dto.TransactionResponse;
import com.sft.transaction.dto.TransactionUpdateRequest;
import com.sft.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final MlClient mlClient;
    private final MlFallbackService fallbackService;
    private final MlProperties mlProperties;
    private final TransactionTemplate txTemplate;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              CategoryService categoryService,
                              MlClient mlClient,
                              MlFallbackService fallbackService,
                              MlProperties mlProperties,
                              TransactionTemplate txTemplate) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.mlClient = mlClient;
        this.fallbackService = fallbackService;
        this.mlProperties = mlProperties;
        this.txTemplate = txTemplate;
    }

    /** Resultado de decidir la categoría: cuál, con qué confianza y quién la decidió. */
    record Categorization(Category category, BigDecimal confidence, CategorySource source) {
    }

    // ------------------------------------------------------------------ lectura

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(Long userId, LocalDate from, LocalDate to, Long categoryId) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));      // aislamiento por usuario
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredOn"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredOn"), to));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("id"));
        return transactionRepository.findAll(spec, sort).stream().map(TransactionResponse::from).toList();
    }

    // ------------------------------------------------------------------ escritura

    /**
     * Crea una transacción.
     *
     * Deliberadamente SIN @Transactional: la llamada HTTP al ML (hasta 800 ms) no debe mantener
     * abierta una conexión de BD. El save() final ya corre en su propia transacción.
     */
    public TransactionResponse create(Long userId, TransactionRequest req) {
        Transaction tx = new Transaction();
        tx.setUser(userRepository.getReferenceById(userId));
        tx.setAmount(req.amount());
        tx.setType(req.type());
        tx.setDescription(req.description().trim());
        tx.setOccurredOn(req.occurredOn());

        if (req.categoryId() != null) {
            // El usuario eligió categoría: no se llama al ML.
            tx.setCategory(categoryService.getVisible(userId, req.categoryId()));
            tx.setCategorySource(CategorySource.MANUAL);
            tx.setMlConfidence(null);
        } else {
            applyCategorization(tx, categorize(userId, tx.getDescription(), tx.getAmount()));
        }
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionUpdateRequest req) {
        Transaction tx = getOwned(userId, id);
        if (req.amount() != null) {
            tx.setAmount(req.amount());
        }
        if (req.type() != null) {
            tx.setType(req.type());
        }
        if (req.description() != null) {
            tx.setDescription(req.description().trim());
        }
        if (req.occurredOn() != null) {
            tx.setOccurredOn(req.occurredOn());
        }
        if (req.categoryId() != null) {
            // Recategorización manual: cambia el origen a MANUAL y borra la confianza del ML.
            tx.setCategory(categoryService.getVisible(userId, req.categoryId()));
            tx.setCategorySource(CategorySource.MANUAL);
            tx.setMlConfidence(null);
        }
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    /**
     * POST /api/transactions/{id}/recategorize: vuelve a pasar la descripción por ML (o reglas).
     *
     * Tres pasos para no mantener una transacción de BD abierta durante la llamada HTTP al ML:
     * leer -> categorizar (sin transacción) -> releer y guardar dentro de una transacción corta.
     * Se relee la entidad dentro de la transacción para trabajar con una instancia gestionada
     * (si se hiciera save() sobre la instancia desligada, Hibernate la fusionaría y la categoría
     * quedaría como proxy perezoso, rompiendo la serialización de la respuesta).
     */
    public TransactionResponse recategorize(Long userId, Long id) {
        Transaction current = getOwned(userId, id);
        Categorization result = categorize(userId, current.getDescription(), current.getAmount());

        return txTemplate.execute(status -> {
            Transaction tx = getOwned(userId, id);
            applyCategorization(tx, result);
            return TransactionResponse.from(transactionRepository.save(tx));
        });
    }

    @Transactional
    public void delete(Long userId, Long id) {
        transactionRepository.delete(getOwned(userId, id));
    }

    // ------------------------------------------------------------------ categorización ML + fallback

    /**
     * Decide la categoría de una descripción. NUNCA lanza por culpa del ML:
     *
     *  1. Llama al ML service (timeout 800 ms + circuit breaker, ver MlClient).
     *  2. Usa su respuesta solo si es una predicción real (source = ML), tiene confianza suficiente
     *     y la categoría existe en la BD  -> source = ML, guarda mlConfidence.
     *  3. En cualquier otro caso (ML caído, lento, sin modelo, poco seguro o con una categoría que no
     *     conocemos) cae a MlFallbackService (palabras clave; "Otros" si nada coincide)
     *     -> source = RULE, sin confianza.
     *
     * Así el endpoint responde 201 aunque el ML esté completamente caído.
     */
    Categorization categorize(Long userId, String description, BigDecimal amount) {
        MlCategoryResponse ml = null;
        try {
            ml = mlClient.predict(description, amount);
        } catch (MlServiceException e) {
            // Solo se registra: es un estado esperado y recuperable, no un error de la petición.
            log.warn("ML no disponible, se usa el fallback por reglas: {}", e.getMessage());
        }

        if (ml != null) {
            if (ml.isRealPrediction() && ml.confidence() >= mlProperties.minConfidence()) {
                Optional<Category> category = categoryService.findVisibleByName(userId, ml.category());
                if (category.isPresent()) {
                    return new Categorization(category.get(), toConfidence(ml.confidence()), CategorySource.ML);
                }
                // Categoría inexistente en la BD: no se crea dinámicamente (evita duplicados y basura).
                // Se descarta la predicción y se cae a reglas, que terminan en "Otros" si no hay match.
                log.warn("El ML devolvió la categoría desconocida '{}'; se usa el fallback", ml.category());
            } else {
                log.debug("Predicción ML no concluyente (source={}, confidence={}); se usa el fallback",
                        ml.source(), ml.confidence());
            }
        }

        String ruleCategoryName = fallbackService.categorize(description);
        Category category = categoryService.findVisibleByName(userId, ruleCategoryName)
                .orElseGet(categoryService::getDefault);
        return new Categorization(category, null, CategorySource.RULE);
    }

    private static void applyCategorization(Transaction tx, Categorization c) {
        tx.setCategory(c.category());
        tx.setCategorySource(c.source());
        tx.setMlConfidence(c.confidence());
    }

    private static BigDecimal toConfidence(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return BigDecimal.valueOf(clamped).setScale(3, RoundingMode.HALF_UP);
    }

    private Transaction getOwned(Long userId, Long id) {
        // Se filtra por usuario: la transacción de otro usuario es un 404, no un 403 (no revela que existe).
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción no encontrada"));
    }
}
