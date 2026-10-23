BEGIN;

CREATE TABLE public.users (
                              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              name VARCHAR(100) NOT NULL CHECK (btrim(name) <> ''),
                              email VARCHAR(254) NOT NULL CHECK (btrim(email) <> '')
);

CREATE UNIQUE INDEX users_email_unique
    ON public.users (lower(btrim(email)));

CREATE TABLE public.recycling_types (
                                        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        name VARCHAR(100) NOT NULL CHECK (btrim(name) <> ''),
                                        unit VARCHAR(10) NOT NULL CHECK (unit = 'кг'),
                                        points_per_kg NUMERIC(9, 2) NOT NULL
                                            CHECK (points_per_kg BETWEEN 0 AND 1000000)
);

CREATE TABLE public.collection_points (
                                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                          address VARCHAR(300) NOT NULL CHECK (btrim(address) <> ''),
                                          available BOOLEAN NOT NULL,
                                          daily_weight_limit NUMERIC(13, 3) NOT NULL
                                              CHECK (daily_weight_limit > 0 AND daily_weight_limit <= 1000000000)
);

CREATE TABLE public.collection_point_types (
                                               point_id BIGINT NOT NULL
                                                   REFERENCES public.collection_points(id) ON DELETE CASCADE,
                                               type_id BIGINT NOT NULL
                                                   REFERENCES public.recycling_types(id) ON DELETE CASCADE,
                                               PRIMARY KEY (point_id, type_id)
);

CREATE TABLE public.recycling_batches (
                                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                          user_id BIGINT NOT NULL
                                              REFERENCES public.users(id) ON DELETE CASCADE,
                                          point_id BIGINT NOT NULL
                                              REFERENCES public.collection_points(id) ON DELETE CASCADE,
                                          type_id BIGINT NOT NULL
                                              REFERENCES public.recycling_types(id) ON DELETE CASCADE,
                                          weight_kg NUMERIC(13, 3) NOT NULL
                                              CHECK (weight_kg > 0 AND weight_kg <= 1000000000),
                                          received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED'
                                              CHECK (status IN ('ACCEPTED', 'CANCELLED')),
                                          points_per_kg NUMERIC(9, 2) NOT NULL
                                              CHECK (points_per_kg BETWEEN 0 AND 1000000)
);

CREATE TABLE public.bonus_entries (
                                      id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      batch_id BIGINT NOT NULL UNIQUE
                                          REFERENCES public.recycling_batches(id) ON DELETE CASCADE,
                                      points NUMERIC(18, 2) NOT NULL
                                          CHECK (points BETWEEN 0 AND 1000000000000000),
                                      status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                          CHECK (status IN ('PENDING', 'ACCRUED', 'CANCELLED'))
);

REVOKE CREATE, TEMPORARY ON DATABASE recycling_db FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT CONNECT ON DATABASE recycling_db TO recycling_app;
GRANT USAGE ON SCHEMA public TO recycling_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
    public.users,
    public.recycling_types,
    public.collection_points,
    public.collection_point_types,
    public.recycling_batches,
    public.bonus_entries
    TO recycling_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO recycling_app;

COMMIT;


-- Примеры для новой пустой базы после schema.sql.
-- Выполнить под postgres. ID получаются из INSERT RETURNING.

BEGIN;

DO $seed$
DECLARE
v_user_1 BIGINT;
    v_user_2 BIGINT;
    v_paper BIGINT;
    v_plastic BIGINT;
    v_glass BIGINT;
    v_point_1 BIGINT;
    v_point_2 BIGINT;
    v_point_3 BIGINT;
    v_batch_1 BIGINT;
    v_batch_2 BIGINT;
    v_batch_3 BIGINT;
BEGIN
INSERT INTO public.users (name, email)
VALUES ('Елисей', 'elisey@example.com')
    RETURNING id INTO v_user_1;

INSERT INTO public.users (name, email)
VALUES ('Анна', 'anna@example.com')
    RETURNING id INTO v_user_2;

INSERT INTO public.recycling_types (name, unit, points_per_kg)
VALUES ('Бумага', 'кг', 3.00)
    RETURNING id INTO v_paper;

INSERT INTO public.recycling_types (name, unit, points_per_kg)
VALUES ('Пластик', 'кг', 5.00)
    RETURNING id INTO v_plastic;

INSERT INTO public.recycling_types (name, unit, points_per_kg)
VALUES ('Стекло', 'кг', 2.00)
    RETURNING id INTO v_glass;

INSERT INTO public.collection_points
(address, available, daily_weight_limit)
VALUES ('ул. Лесная, 10', TRUE, 1000.000)
    RETURNING id INTO v_point_1;

INSERT INTO public.collection_points
(address, available, daily_weight_limit)
VALUES ('ул. Садовая, 5', TRUE, 500.000)
    RETURNING id INTO v_point_2;

INSERT INTO public.collection_points
(address, available, daily_weight_limit)
VALUES ('ул. Парковая, 7', FALSE, 300.000)
    RETURNING id INTO v_point_3;

INSERT INTO public.collection_point_types (point_id, type_id)
VALUES
    (v_point_1, v_paper),
    (v_point_1, v_plastic),
    (v_point_2, v_plastic),
    (v_point_2, v_glass),
    (v_point_3, v_paper),
    (v_point_3, v_glass);

INSERT INTO public.recycling_batches
(user_id, point_id, type_id, weight_kg, status, points_per_kg)
VALUES (v_user_1, v_point_1, v_paper, 10.000, 'ACCEPTED', 3.00)
    RETURNING id INTO v_batch_1;

INSERT INTO public.recycling_batches
(user_id, point_id, type_id, weight_kg, status, points_per_kg)
VALUES (v_user_2, v_point_1, v_plastic, 4.000, 'ACCEPTED', 5.00)
    RETURNING id INTO v_batch_2;

INSERT INTO public.recycling_batches
(user_id, point_id, type_id, weight_kg, status, points_per_kg)
VALUES (v_user_1, v_point_2, v_glass, 2.000, 'CANCELLED', 2.00)
    RETURNING id INTO v_batch_3;

INSERT INTO public.bonus_entries (batch_id, points, status)
VALUES
    (v_batch_1, 30.00, 'ACCRUED'),
    (v_batch_2, 20.00, 'PENDING'),
    (v_batch_3, 4.00, 'CANCELLED');
END;
$seed$;

COMMIT;
