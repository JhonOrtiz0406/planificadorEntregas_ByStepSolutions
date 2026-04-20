-- ========================================
-- V8: Category statuses for LAUNDRY and PHONES
-- ByStep Solutions S.A.S.
-- ========================================

-- LAUNDRY
INSERT INTO category_statuses (category_id, status_key, label, display_order, is_final) VALUES
    ('LAUNDRY', 'NOT_STARTED',  'Sin iniciar',           1, false),
    ('LAUNDRY', 'RECEIVED',     'Recibido',               2, false),
    ('LAUNDRY', 'WASHING',      'En lavado',              3, false),
    ('LAUNDRY', 'DRYING',       'En secado / planchado',  4, false),
    ('LAUNDRY', 'READY',        'Listo para entrega',     5, false),
    ('LAUNDRY', 'DELIVERED',    'Entregado',              6, true)
ON CONFLICT (category_id, status_key) DO NOTHING;

-- PHONES
INSERT INTO category_statuses (category_id, status_key, label, display_order, is_final) VALUES
    ('PHONES', 'NOT_STARTED',  'Sin iniciar',         1, false),
    ('PHONES', 'DIAGNOSING',   'Diagnosticando',       2, false),
    ('PHONES', 'WAITING_PART', 'Esperando repuesto',   3, false),
    ('PHONES', 'REPAIRING',    'En reparación',        4, false),
    ('PHONES', 'READY',        'Listo para entrega',   5, false),
    ('PHONES', 'DELIVERED',    'Entregado',            6, true)
ON CONFLICT (category_id, status_key) DO NOTHING;
