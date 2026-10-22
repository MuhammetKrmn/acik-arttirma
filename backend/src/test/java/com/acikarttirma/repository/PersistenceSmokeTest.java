package com.acikarttirma.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * F2 smoke testi: entity'ler şemayla uyuşuyor mu (ddl-auto=validate), dev seed doğru mu,
 * oda zinciri yazılıp okunabiliyor mu.
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

    @Test
    void herMevkiVeTierHavuzundaEnAz3KartVar() {
        Long conceptId = devConcept().getId();
        for (Position position : Position.values()) {
            for (Tier tier : Tier.values()) {
                int count = cardRepository
                        .findByConcept_IdAndPositionAndTierAndPenaltyCardFalse(conceptId, position, tier)
                        .size();
                assertTrue(count >= 3,
                        position + " " + tier + " havuzunda en az 3 kart olmalı, bulunan: " + count);
            }
        }
    }

    @Test
    void u19KartlariMevkiBasinaTekVeUcretsiz() {
        List<Card> penalty = cardRepository.findByPenaltyCardTrue();
        assertEquals(Position.values().length, penalty.size());
        assertEquals(penalty.size(), penalty.stream().map(Card::getPosition).distinct().count());
        for (Card card : penalty) {
            assertEquals(0, card.getStartingPrice());
            assertNull(card.getConcept());
            assertNull(card.getFootballer());
        }
    }

    @Test
    void tier3AcilisFiyatiKurali() {
        List<Card> all = cardRepository.findByConcept_IdAndPenaltyCardFalse(devConcept().getId());
        for (Card card : all) {
            if (card.getTier() != Tier.T3) {
                continue;
            }
            boolean attack = card.getPosition() == Position.LW
                    || card.getPosition() == Position.ST
                    || card.getPosition() == Position.RW;
            assertEquals(attack ? 30 : 20, card.getStartingPrice(),
                    card.getPosition() + " tier 3 açılış fiyatı");
        }
    }

    @Test
    void odaZinciriYazilipOkunuyor() {
        Concept concept = devConcept();
        Card stCard = cardRepository
                .findByConcept_IdAndPositionAndTierAndPenaltyCardFalse(concept.getId(), Position.ST, Tier.T1)
                .get(0);
        Card u19Cb = cardRepository.findByPositionAndPenaltyCardTrue(Position.CB).get(0);

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