package com.acikarttirma.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.model.AuctionItem;
import com.acikarttirma.model.Card;
import com.acikarttirma.model.Concept;
import com.acikarttirma.model.Room;
import com.acikarttirma.model.RoomPlayer;
import com.acikarttirma.model.SquadPlayer;
import com.acikarttirma.model.User;
import com.acikarttirma.model.UserSquad;
import com.acikarttirma.model.enums.AcquisitionType;
import com.acikarttirma.model.enums.Position;
import com.acikarttirma.model.enums.RoomStatus;
import com.acikarttirma.model.enums.Slot;
import com.acikarttirma.model.enums.Tier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * F2 smoke testi: entity'ler şemayla uyuşuyor mu (ddl-auto=validate), seed ve gerçek veri
 * kurallara uyuyor mu, oda zinciri yazılıp okunabiliyor mu.
 *
 * <p>Havuz ve fiyat kontrolleri kartı olan HER konsepte uygulanır (dev ve gerçek veri).
 * Henüz kartı girilmemiş konseptler atlanır.
 *
 * <p>Docker'daki PostgreSQL açık olmalı (docker compose up -d). Her test sonunda geri alınır,
 * veritabanına kalıcı bir şey yazmaz. F7'de Testcontainers'a geçilecek.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class PersistenceSmokeTest {

    private static final String DEV_CONCEPT = "DEV Test Konsepti";

    @PersistenceContext
    private EntityManager em;

    @Autowired private ConceptRepository conceptRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomPlayerRepository roomPlayerRepository;
    @Autowired private AuctionItemRepository auctionItemRepository;
    @Autowired private UserSquadRepository userSquadRepository;
    @Autowired private SquadPlayerRepository squadPlayerRepository;

    private Concept devConcept() {
        return conceptRepository.findByConceptName(DEV_CONCEPT).orElseThrow();
    }

    /** Normal kartı girilmiş konseptler (boş konseptler atlanır). */
    private List<Concept> conceptsWithCards() {
        return conceptRepository.findAll().stream()
                .filter(c -> !cardRepository.findByConcept_IdAndPenaltyCardFalse(c.getId()).isEmpty())
                .toList();
    }

    @Test
    void herMevkiVeTierHavuzundaEnAz3KartVar() {
        for (Concept concept : conceptsWithCards()) {
            for (Position position : Position.values()) {
                for (Tier tier : Tier.values()) {
                    int count = cardRepository
                            .findByConcept_IdAndPositionAndTierAndPenaltyCardFalse(
                                    concept.getId(), position, tier)
                            .size();
                    assertTrue(count >= 3, concept.getConceptName() + " " + position + " " + tier
                            + " havuzunda en az 3 kart olmalı, bulunan: " + count);
                }
            }
        }
    }

    @Test
    void herKonseptinU19HavuzundaHerMevkideEnAz3KartVar() {
        for (Concept concept : conceptsWithCards()) {
            for (Position position : Position.values()) {
                List<Card> pool = cardRepository
                        .findByConcept_IdAndPositionAndPenaltyCardTrue(concept.getId(), position);
                assertTrue(pool.size() >= 3, concept.getConceptName() + " " + position
                        + " U19 havuzunda en az 3 kart olmalı, bulunan: " + pool.size());
                for (Card card : pool) {
                    assertEquals(0, card.getStartingPrice());
                    assertNull(card.getTier());
                    assertNotNull(card.getFootballer());
                }
            }
        }
    }

    @Test
    void tier3AcilisFiyatiKurali() {
        for (Concept concept : conceptsWithCards()) {
            List<Card> all = cardRepository.findByConcept_IdAndPenaltyCardFalse(concept.getId());
            for (Card card : all) {
                if (card.getTier() != Tier.T3) {
                    continue;
                }
                boolean attack = card.getPosition() == Position.LW
                        || card.getPosition() == Position.ST
                        || card.getPosition() == Position.RW;
                assertEquals(attack ? 30 : 20, card.getStartingPrice(),
                        concept.getConceptName() + " " + card.getPosition() + " tier 3 açılış fiyatı");
            }
        }
    }

    @Test
    void odaZinciriYazilipOkunuyor() {
        Concept concept = devConcept();
        Card stCard = cardRepository
                .findByConcept_IdAndPositionAndTierAndPenaltyCardFalse(concept.getId(), Position.ST, Tier.T1)
                .get(0);
        Card u19Cb = cardRepository
                .findByConcept_IdAndPositionAndPenaltyCardTrue(concept.getId(), Position.CB).get(0);

        User user = userRepository.save(new User("smoke_" + UUID.randomUUID().toString().substring(0, 8)));
        Room room = roomRepository.save(new Room(concept, 1000, 42L));
        RoomPlayer player = roomPlayerRepository.save(new RoomPlayer(room, user, (short) 1, 1000));
        UserSquad squad = userSquadRepository.save(new UserSquad(player));

        squadPlayerRepository.save(new SquadPlayer(squad, stCard, Slot.ST, 55, AcquisitionType.AUCTION));
        squadPlayerRepository.save(new SquadPlayer(squad, u19Cb, Slot.CB_1, 0, AcquisitionType.U19_PENALTY));
        auctionItemRepository.save(
                new AuctionItem(room, stCard, stCard.getFootballer(), Slot.ST, Tier.T1, (short) 1));

        em.flush();
        em.clear();

        Long roomId = room.getId();
        assertEquals(RoomStatus.LOBBY, roomRepository.findById(roomId).orElseThrow().getStatus());

        List<AuctionItem> items = auctionItemRepository.findByRoom_IdOrderBySequenceNo(roomId);
        assertEquals(1, items.size());
        assertEquals(Tier.T1, items.get(0).getTier());
        assertEquals(Slot.ST, items.get(0).getSlot());

        assertEquals(1, roomPlayerRepository.findByRoom_IdOrderBySeatNumber(roomId).size());
        UserSquad loadedSquad = userSquadRepository.findByRoomPlayer_Id(player.getId()).orElseThrow();
        assertEquals(2, squadPlayerRepository.findBySquad_Id(loadedSquad.getId()).size());
    }
}