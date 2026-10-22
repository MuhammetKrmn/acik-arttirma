-- V900__dev_seed.sql  (SADECE dev profili)
-- Geliştirme için sahte veri: 1 konsept, her mevki x tier için 4 kart (toplam 120 kart)
-- ve her mevki için 4 U19 ceza kartı (toplam 40 kart, konsepte özel havuz).
-- Gerçek konsept ve futbolcu verisini veritabanı sorumlusu ayrıca girer.
-- starting_price değerleri geçicidir (T1=50, T2=30), T3 kuralı gerçektir:
-- hücum (LW, ST, RW) 30, diğerleri 20.

INSERT INTO concepts (concept_name, description)
VALUES ('DEV Test Konsepti', 'Geliştirme için sahte veri. Üretimde kullanılmaz.');

WITH gen AS (
    SELECT p.pos,
           t.tier,
           n.n,
           format('DEV %s T%s #%s', p.pos, t.tier, n.n) AS nm
    FROM unnest(ARRAY['GK','LB','CB','RB','CDM','CM','CAM','LW','ST','RW']) AS p(pos)
             CROSS JOIN generate_series(1, 3) AS t(tier)
             CROSS JOIN generate_series(1, 4) AS n(n)
),
     ins AS (
INSERT INTO footballers (player_name, nationality)
SELECT nm, 'Türkiye' FROM gen
                              RETURNING footballer_id, player_name
)
INSERT INTO cards (concept_id, footballer_id, club, position, tier,
                   overall_rating, starting_price, is_penalty_card)
SELECT c.concept_id,
       ins.footballer_id,
       'DEV FC',
       g.pos,
       g.tier,
       95 - g.tier * 10 + g.n,
       CASE
           WHEN g.tier = 1 THEN 50
           WHEN g.tier = 2 THEN 30
           WHEN g.pos IN ('LW', 'ST', 'RW') THEN 30
           ELSE 20
           END,
       FALSE
FROM gen g
         JOIN ins ON ins.player_name = g.nm
         CROSS JOIN (SELECT concept_id FROM concepts WHERE concept_name = 'DEV Test Konsepti') c;

-- U19 ceza havuzu (DEV konsepti için): her mevkide 4 kart, fiyat 0, tier boş.
WITH gen AS (
    SELECT p.pos, n.n, format('DEV %s U19 #%s', p.pos, n.n) AS nm
    FROM unnest(ARRAY['GK','LB','CB','RB','CDM','CM','CAM','LW','ST','RW']) AS p(pos)
             CROSS JOIN generate_series(1, 4) AS n(n)
),
     ins AS (
INSERT INTO footballers (player_name, nationality)
SELECT nm, 'Türkiye' FROM gen
                              RETURNING footballer_id, player_name
)
INSERT INTO cards (concept_id, footballer_id, club, position, tier,
                   overall_rating, starting_price, is_penalty_card)
SELECT c.concept_id, ins.footballer_id, 'DEV U19', g.pos, NULL, 45, 0, TRUE
FROM gen g
         JOIN ins ON ins.player_name = g.nm
         CROSS JOIN (SELECT concept_id FROM concepts WHERE concept_name = 'DEV Test Konsepti') c;

