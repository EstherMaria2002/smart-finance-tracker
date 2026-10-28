package com.sft.ml;

import com.sft.category.Category;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Clasificador por reglas de palabra clave. Se usa cuando el ML service falla, tarda más de 800 ms
 * o no está seguro. Así el sistema sigue funcionando sin ML (decisión de diseño: el fallback vive
 * en Spring, no en Python).
 *
 * Sintaxis de las palabras clave (ya normalizadas: minúsculas y sin tildes):
 *   "cena"      -> coincide con la palabra exacta "cena"
 *   "cena*"     -> coincide con cualquier palabra que EMPIECE por "cena" (cenar, cenas...)
 *   "uber eats" -> frase de varias palabras
 * Se evalúan en orden: gana la primera categoría con coincidencia (por eso "uber eats"
 * va en Restaurantes, antes que "uber" en Transporte). Si nada coincide -> "Otros".
 * Los nombres de categoría deben existir en V2__seed_categories.sql.
 */
@Service
public class MlFallbackService {

    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("Nómina", List.of("nomina*", "salario*", "sueldo*", "paga extra", "payroll", "salary"));
        RULES.put("Restaurantes", List.of("cena*", "almuerzo*", "desayuno*", "restaurante*", "cafe", "cafes",
                "cafeteria*", "bar", "tapas", "pizz*", "hamburgues*", "burger*", "kebab*", "sushi", "brunch",
                "menu del dia", "mcdonald*", "telepizza", "glovo", "just eat", "uber eats"));
        RULES.put("Suscripciones", List.of("netflix", "spotify", "hbo", "disney*", "prime video",
                "youtube premium", "suscripcion*", "icloud", "chatgpt", "apple music"));
        RULES.put("Transporte", List.of("uber", "taxi*", "cabify", "bolt", "gasolina*", "gasolinera*",
                "combustible", "diesel", "metro", "autobus*", "bus", "tren*", "renfe", "parking",
                "aparcamiento*", "peaje*", "blablacar", "cercanias"));
        RULES.put("Ocio", List.of("cine", "concierto*", "teatro", "videojuego*", "steam", "playstation", "xbox",
                "nintendo", "discoteca*", "museo", "entradas", "festival", "bolera"));
        RULES.put("Supermercado", List.of("mercadona", "carrefour", "lidl", "aldi", "dia", "supermercado*",
                "alcampo", "eroski", "hipercor", "consum", "compra semanal", "fruteria*"));
        RULES.put("Salud", List.of("farmacia*", "medico*", "dentista*", "hospital*", "clinica*", "fisio*",
                "optica", "psicolog*", "analitica"));
        RULES.put("Hogar", List.of("alquiler", "hipoteca", "luz", "agua", "gas", "internet", "fibra",
                "comunidad", "ikea", "fontaner*", "electricidad", "iberdrola", "endesa", "reparacion*", "muebles"));
        RULES.put("Educación", List.of("curso*", "universidad", "matricula", "colegio", "libro*", "udemy",
                "academia", "master", "clases"));
        RULES.put("Ropa", List.of("zara", "ropa", "camiseta*", "zapatilla*", "pantalon*", "primark", "nike",
                "adidas", "zapato*", "vestido*", "abrigo*"));
        RULES.put("Viajes", List.of("vuelo*", "hotel*", "airbnb", "booking", "ryanair", "vueling", "iberia",
                "hostal", "viaje*", "avion"));
    }

    /** @return nombre de la categoría según palabras clave, o "Otros" si nada coincide. */
    public String categorize(String description) {
        String normalized = normalize(description);
        if (normalized.isEmpty()) {
            return Category.DEFAULT_NAME;
        }
        Set<String> tokens = Arrays.stream(normalized.split(" ")).collect(Collectors.toSet());
        String padded = " " + normalized + " ";

        for (Map.Entry<String, List<String>> rule : RULES.entrySet()) {
            for (String keyword : rule.getValue()) {
                if (matches(keyword, tokens, padded)) {
                    return rule.getKey();
                }
            }
        }
        return Category.DEFAULT_NAME;
    }

    private static boolean matches(String keyword, Set<String> tokens, String paddedText) {
        if (keyword.contains(" ")) {
            return paddedText.contains(" " + keyword + " ");
        }
        if (keyword.endsWith("*")) {
            String prefix = keyword.substring(0, keyword.length() - 1);
            return tokens.stream().anyMatch(t -> t.startsWith(prefix));
        }
        return tokens.contains(keyword);
    }

    /** minúsculas, sin tildes, solo [a-z0-9] separados por un espacio. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String noAccents = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccents.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
