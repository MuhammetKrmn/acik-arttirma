-- V2__penalty_cards.sql
-- U19 ceza kartları: tier 3 atamasında bütçe yetmeyen oyuncuya SADECE o slot için verilir.
-- Havuza girmez, konsepte bağlı değildir, mevki başına tek kart (ux_cards_penalty_per_position),
-- fiyat 0. CB için tek kart vardır, CB_1 ve CB_2 slotlarında ikisinde de kullanılır.
-- overall_rating 50 sabit bir kabuldür: değiştirilecekse yeni bir migration yazılır.
INSERT INTO cards (club, position, tier, overall_rating, starting_price, is_penalty_card)
SELECT 'U19', pos, 3, 50, 0, TRUE
FROM unnest(ARRAY['GK','LB','CB','RB','CDM','CM','CAM','LW','ST','RW']) AS pos;