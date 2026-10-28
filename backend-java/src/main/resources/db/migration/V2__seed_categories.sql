-- V2: categorías del sistema (user_id = NULL, is_system = true).
-- IMPORTANTE: estos nombres deben coincidir EXACTAMENTE con las clases del modelo
-- ML (ml-service-python/app/data/seed_transactions.csv) y con las reglas de
-- MlFallbackService. "Otros" es la categoría por defecto cuando nada encaja.

INSERT INTO categories (name, color, icon, user_id, is_system) VALUES
    ('Restaurantes',  '#FF6B6B', '🍽️', NULL, true),
    ('Supermercado',  '#F59F00', '🛒', NULL, true),
    ('Transporte',    '#339AF0', '🚗', NULL, true),
    ('Ocio',          '#9775FA', '🎬', NULL, true),
    ('Salud',         '#20C997', '💊', NULL, true),
    ('Hogar',         '#845EF7', '🏠', NULL, true),
    ('Educación',     '#4C6EF5', '📚', NULL, true),
    ('Ropa',          '#F06595', '👕', NULL, true),
    ('Suscripciones', '#15AABF', '🔁', NULL, true),
    ('Viajes',        '#FF922B', '✈️', NULL, true),
    ('Nómina',        '#2F9E44', '💼', NULL, true),
    ('Otros',         '#868E96', '📦', NULL, true);
