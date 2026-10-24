-- Kimsenin açamadığı ihale (slot dolu veya bütçe yetersiz) satılmaz, UNSOLD olarak işaretlenir.
ALTER TABLE auction_items DROP CONSTRAINT ck_auction_items_status;
ALTER TABLE auction_items
    ADD CONSTRAINT ck_auction_items_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'SOLD', 'AUTO_ASSIGNED', 'UNSOLD'));