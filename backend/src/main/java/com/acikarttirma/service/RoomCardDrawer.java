package com.acikarttirma.service;

import com.acikarttirma.model.AuctionItem;
import com.acikarttirma.model.Card;
import com.acikarttirma.model.Room;
import com.acikarttirma.model.enums.Slot;
import com.acikarttirma.model.enums.Tier;
import com.acikarttirma.repository.AuctionItemRepository;
import com.acikarttirma.repository.CardRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Odanın 33 kartını çeker: 11 slot x 3 tier. T1 ve T2 (22 kart) karıştırılıp sequence_no alır,
 * T3 (11 kart) ihaleye çıkmaz, sequence_no boş kalır. Aynı seed her zaman aynı sonucu verir.
 */
@Component
public class RoomCardDrawer {

    private record Pick(Slot slot, Tier tier, Card card) {
    }

    private final CardRepository cardRepository;
    private final AuctionItemRepository auctionItemRepository;

    public RoomCardDrawer(CardRepository cardRepository, AuctionItemRepository auctionItemRepository) {
        this.cardRepository = cardRepository;
        this.auctionItemRepository = auctionItemRepository;
    }

    public void draw(Room room) {
        Random random = new Random(room.getSeed());

        List<Card> all = new ArrayList<>(
                cardRepository.findByConcept_IdAndPenaltyCardFalse(room.getConcept().getId()));
        all.sort(Comparator.comparing(Card::getId)); // seed'in tekrarlanabilir olması için sabit sıra

        Set<Long> used = new HashSet<>();
        List<Pick> auctioned = new ArrayList<>();
        List<Pick> tier3 = new ArrayList<>();

        for (Slot slot : Slot.values()) {
            for (Tier tier : Tier.values()) {
                List<Card> pool = all.stream()
                        .filter(c -> c.getPosition() == slot.getPosition()
                                && c.getTier() == tier
                                && !used.contains(c.getId()))
                        .toList();
                if (pool.isEmpty()) {
                    throw new IllegalStateException("Havuz yetersiz: " + slot + " " + tier);
                }
                Card picked = pool.get(random.nextInt(pool.size()));
                used.add(picked.getId());
                (tier == Tier.T3 ? tier3 : auctioned).add(new Pick(slot, tier, picked));
            }
        }

        Collections.shuffle(auctioned, random);

        List<AuctionItem> items = new ArrayList<>();
        short sequence = 1;
        for (Pick p : auctioned) {
            items.add(new AuctionItem(room, p.card(), p.card().getFootballer(), p.slot(), p.tier(), sequence++));
        }
        for (Pick p : tier3) {
            items.add(new AuctionItem(room, p.card(), p.card().getFootballer(), p.slot(), p.tier(), null));
        }
        auctionItemRepository.saveAll(items);
    }
}