-- =====================================================================
-- V1__init.sql  (TASLAK - veritabanı sorumlusu gözden geçirecek)
-- Açık Arttırma Oyunu - v1 şeması (PostgreSQL 16)
--
-- Notlar:
--  * Parasal değerler milyon € cinsinden TAM SAYI (INTEGER).
--  * Ihale tekliflerinin kendisi (bids) v1'de bellekte tutulur, tablosu yok.
--  * overall_rating sadece sunucuda okunur, hiçbir API cevabına konmaz.
--  * rooms.winner_room_player_id döngüsel FK: tablo sonunda ALTER ile eklenir.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) concepts: oyun konseptleri (~35 adet)
-- ---------------------------------------------------------------------
CREATE TABLE concepts (
                          concept_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          concept_name  VARCHAR(100) NOT NULL,
                          description   TEXT,
                          photo_url     VARCHAR(500),
                          is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                          CONSTRAINT uq_concepts_name UNIQUE (concept_name)
);

-- ---------------------------------------------------------------------
-- 2) footballers: gerçek futbolcular (konseptten bağımsız kimlik bilgisi)
-- ---------------------------------------------------------------------
CREATE TABLE footballers (
                             footballer_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             player_name   VARCHAR(150) NOT NULL,
                             nationality   VARCHAR(100),
                             photo_url     VARCHAR(500),
                             birth_date    DATE
);

-- ---------------------------------------------------------------------
-- 3) cards: konsept + mevki + tier için kart havuzu
--    Normal kart : concept_id ve footballer_id dolu, starting_price > 0
--    U19 ceza kartı: concept_id ve footballer_id NULL, starting_price = 0,
--                    havuza girmez, her mevki için tek kart
--    (CB için tek U19 kartı vardır, CB_1 ve CB_2 slotlarında ikisinde de kullanılabilir)
-- ---------------------------------------------------------------------
CREATE TABLE cards (
                       card_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       concept_id       BIGINT REFERENCES concepts (concept_id),
                       footballer_id    BIGINT REFERENCES footballers (footballer_id),
                       club             VARCHAR(100),
                       position         VARCHAR(3)  NOT NULL,
                       tier             SMALLINT    NOT NULL,
                       overall_rating   SMALLINT    NOT NULL,
                       starting_price   INTEGER     NOT NULL,
                       is_penalty_card  BOOLEAN     NOT NULL DEFAULT FALSE,

                       CONSTRAINT uq_cards_concept_footballer UNIQUE (concept_id, footballer_id),
                       CONSTRAINT ck_cards_position CHECK (position IN
                                                           ('GK','LB','CB','RB','CDM','CM','CAM','LW','ST','RW')),
                       CONSTRAINT ck_cards_tier CHECK (tier IN (1, 2, 3)),
                       CONSTRAINT ck_cards_rating CHECK (overall_rating BETWEEN 1 AND 99),
                       CONSTRAINT ck_cards_penalty_shape CHECK (
                           (is_penalty_card
                               AND concept_id IS NULL AND footballer_id IS NULL AND starting_price = 0)
                               OR
                           (NOT is_penalty_card
                               AND concept_id IS NOT NULL AND footballer_id IS NOT NULL AND starting_price > 0)
                           )
);

-- U19 ceza kartı: mevki başına tek kart
CREATE UNIQUE INDEX ux_cards_penalty_per_position
    ON cards (position) WHERE is_penalty_card;

-- Oda başında havuzdan rastgele çekim için
CREATE INDEX ix_cards_pool ON cards (concept_id, position, tier);

-- ---------------------------------------------------------------------
-- 4) users: misafir girişli kullanıcılar (v1'de Spring Security yok)
-- ---------------------------------------------------------------------
CREATE TABLE users (
                       user_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       username       VARCHAR(50)  NOT NULL,
                       is_guest       BOOLEAN      NOT NULL DEFAULT TRUE,
                       email          VARCHAR(255),
                       password_hash  VARCHAR(255),
                       created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT uq_users_email UNIQUE (email)
);

-- ---------------------------------------------------------------------
-- 5) rooms
--    status: LOBBY -> SETUP -> AUCTION_ACTIVE -> AUCTION_CLOSED
--            -> TIER3_ASSIGN -> (sonraki ihale) -> FINISHED
-- ---------------------------------------------------------------------
CREATE TABLE rooms (
                       room_id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       concept_id              BIGINT       NOT NULL REFERENCES concepts (concept_id),
                       status                  VARCHAR(20)  NOT NULL DEFAULT 'LOBBY',
                       starting_budget         INTEGER      NOT NULL DEFAULT 1000,
                       seed                    BIGINT,
                       created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       finished_at             TIMESTAMPTZ,
                       winner_room_player_id   BIGINT,
                       CONSTRAINT ck_rooms_status CHECK (status IN
                                                         ('LOBBY','SETUP','AUCTION_ACTIVE','AUCTION_CLOSED','TIER3_ASSIGN','FINISHED')),
                       CONSTRAINT ck_rooms_budget CHECK (starting_budget > 0)
);

-- ---------------------------------------------------------------------
-- 6) room_players: odadaki 3 oyuncu
-- ---------------------------------------------------------------------
CREATE TABLE room_players (
                              room_player_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              room_id         BIGINT   NOT NULL REFERENCES rooms (room_id),
                              user_id         BIGINT   NOT NULL REFERENCES users (user_id),
                              seat_number     SMALLINT NOT NULL,
                              current_budget  INTEGER  NOT NULL,
                              CONSTRAINT uq_room_players_seat UNIQUE (room_id, seat_number),
                              CONSTRAINT uq_room_players_user UNIQUE (room_id, user_id),
                              CONSTRAINT ck_room_players_seat CHECK (seat_number BETWEEN 1 AND 3),
                              CONSTRAINT ck_room_players_budget CHECK (current_budget >= 0)
);

CREATE INDEX ix_room_players_user ON room_players (user_id);

-- Döngüsel FK (rooms <-> room_players): şimdi eklenir.
-- Beraberlikte NULL kalabilir.
ALTER TABLE rooms
    ADD CONSTRAINT fk_rooms_winner
        FOREIGN KEY (winner_room_player_id) REFERENCES room_players (room_player_id);

-- ---------------------------------------------------------------------
-- 7) auction_items: odaya çekilen 33 kart (22 ihale + 11 tier 3)
--    tier 3 kartlarında sequence_no NULL, 1-2 tierlerde dolu.
-- ---------------------------------------------------------------------
CREATE TABLE auction_items (
                               auction_item_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               room_id                 BIGINT      NOT NULL REFERENCES rooms (room_id),
                               card_id                 BIGINT      NOT NULL REFERENCES cards (card_id),
                               footballer_id           BIGINT      NOT NULL REFERENCES footballers (footballer_id),
                               slot_code               VARCHAR(5)  NOT NULL,
                               tier                    SMALLINT    NOT NULL,
                               sequence_no             SMALLINT,
                               status                  VARCHAR(15) NOT NULL DEFAULT 'PENDING',
                               sold_price              INTEGER,
                               winning_room_player_id  BIGINT REFERENCES room_players (room_player_id),

                               CONSTRAINT uq_auction_items_card        UNIQUE (room_id, card_id),
                               CONSTRAINT uq_auction_items_footballer  UNIQUE (room_id, footballer_id),
                               CONSTRAINT uq_auction_items_sequence    UNIQUE (room_id, sequence_no),
                               CONSTRAINT uq_auction_items_slot_tier   UNIQUE (room_id, slot_code, tier),
                               CONSTRAINT ck_auction_items_slot CHECK (slot_code IN
                                                                       ('GK','LB','CB_1','CB_2','RB','CDM','CM','CAM','LW','ST','RW')),
                               CONSTRAINT ck_auction_items_tier CHECK (tier IN (1, 2, 3)),
                               CONSTRAINT ck_auction_items_status CHECK (status IN
                                                                         ('PENDING','ACTIVE','SOLD','AUTO_ASSIGNED')),
                               CONSTRAINT ck_auction_items_sequence CHECK ((tier = 3) = (sequence_no IS NULL)),
                               CONSTRAINT ck_auction_items_price CHECK (sold_price IS NULL OR sold_price >= 0)
);

CREATE INDEX ix_auction_items_room_status ON auction_items (room_id, status);

-- ---------------------------------------------------------------------
-- 8) user_squads: oyuncu başına bir kadro
-- ---------------------------------------------------------------------
CREATE TABLE user_squads (
                             squad_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             room_player_id  BIGINT  NOT NULL REFERENCES room_players (room_player_id),
                             total_spent     INTEGER NOT NULL DEFAULT 0,
                             total_rating    INTEGER NOT NULL DEFAULT 0,
                             CONSTRAINT uq_user_squads_room_player UNIQUE (room_player_id),
                             CONSTRAINT ck_user_squads_spent CHECK (total_spent >= 0)
);

-- ---------------------------------------------------------------------
-- 9) squad_players: kadrodaki kartlar (slot başına bir kart)
--    card_id'de global UNIQUE yok: U19 kartı birden çok kadroda olabilir.
-- ---------------------------------------------------------------------
CREATE TABLE squad_players (
                               squad_player_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               squad_id          BIGINT      NOT NULL REFERENCES user_squads (squad_id),
                               card_id           BIGINT      NOT NULL REFERENCES cards (card_id),
                               slot              VARCHAR(5)  NOT NULL,
                               paid_price        INTEGER     NOT NULL DEFAULT 0,
                               acquisition_type  VARCHAR(15) NOT NULL,
                               CONSTRAINT uq_squad_players_slot UNIQUE (squad_id, slot),
                               CONSTRAINT ck_squad_players_slot CHECK (slot IN
                                                                       ('GK','LB','CB_1','CB_2','RB','CDM','CM','CAM','LW','ST','RW')),
                               CONSTRAINT ck_squad_players_price CHECK (paid_price >= 0),
                               CONSTRAINT ck_squad_players_type CHECK (acquisition_type IN
                                                                       ('AUCTION','TIER3_AUTO','U19_PENALTY'))
);

CREATE INDEX ix_squad_players_squad ON squad_players (squad_id);