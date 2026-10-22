-- V3__u19_havuzu_semasi.sql
-- U19 ceza kartları artık KONSEPTE ÖZEL havuzdur: her konsept için her mevkide birkaç U19 kartı.
-- Oyuncu bir slotun Tier 3 fiyatını karşılayamazsa, o konseptin o mevkideki U19 havuzundan
-- rastgele bir kart verilir (fiyat 0, ihaleye çıkmaz).
-- Veri dosyası (V4) bu şemaya göre yüklenir.

-- 1) V2'deki genel (konseptsiz) U19 kartları artık kullanılmıyor
DELETE FROM cards WHERE is_penalty_card AND concept_id IS NULL;

-- 2) Kısıtlar: mevki başına tek U19 kuralı kalkıyor, U19 kartının tier'ı boş olur
DROP INDEX IF EXISTS ux_cards_penalty_per_position;
ALTER TABLE cards DROP CONSTRAINT ck_cards_penalty_shape;
ALTER TABLE cards DROP CONSTRAINT ck_cards_tier;
ALTER TABLE cards ALTER COLUMN tier DROP NOT NULL;
ALTER TABLE cards ADD CONSTRAINT ck_cards_tier CHECK (tier IS NULL OR tier IN (1, 2, 3));
ALTER TABLE cards ADD CONSTRAINT ck_cards_penalty_shape CHECK (
    (is_penalty_card
        AND tier IS NULL AND starting_price = 0
        AND concept_id IS NOT NULL AND footballer_id IS NOT NULL)
        OR
    (NOT is_penalty_card
        AND tier IS NOT NULL AND starting_price > 0
        AND concept_id IS NOT NULL AND footballer_id IS NOT NULL)
    );

-- Konseptin bir mevkideki U19 havuzunu hızlı bulmak için
CREATE INDEX ix_cards_u19_pool ON cards (concept_id, position) WHERE is_penalty_card;

-- 3) Kart tablosu genişletmeleri
ALTER TABLE cards ALTER COLUMN club TYPE VARCHAR(500);

-- Alt özellikler. GİZLİLİK: overall_rating gibi bunlar da oyun bitene kadar
-- hiçbir REST/WebSocket cevabında gösterilmez (genel reyting tahmin edilebilir).
ALTER TABLE cards
    ADD COLUMN pac INT,
    ADD COLUMN sho INT,
    ADD COLUMN pas INT,
    ADD COLUMN dri INT,
    ADD COLUMN def INT,
    ADD COLUMN phy INT;