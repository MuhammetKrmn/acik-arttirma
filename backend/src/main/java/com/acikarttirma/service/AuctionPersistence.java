package com.acikarttirma.service;

import com.acikarttirma.dto.AuctionPayloads;
import com.acikarttirma.engine.Tier3Assignment;
import com.acikarttirma.engine.U19Picker;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.AuctionItem;
import com.acikarttirma.model.Card;
import com.acikarttirma.model.Footballer;
import com.acikarttirma.model.Room;
import com.acikarttirma.model.RoomPlayer;
import com.acikarttirma.model.SquadPlayer;
import com.acikarttirma.model.UserSquad;
import com.acikarttirma.model.enums.AcquisitionType;
import com.acikarttirma.model.enums.AuctionStatus;
import com.acikarttirma.model.enums.RoomStatus;
import com.acikarttirma.model.enums.Slot;
import com.acikarttirma.model.enums.Tier;
import com.acikarttirma.repository.AuctionItemRepository;
import com.acikarttirma.repository.CardRepository;
import com.acikarttirma.repository.RoomPlayerRepository;
import com.acikarttirma.repository.RoomRepository;
import com.acikarttirma.repository.SquadPlayerRepository;
import com.acikarttirma.repository.UserSquadRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Canlı ihalenin veritabanı işleri. Her metot kendi transaction'ında çalışır ve kısa sürer;
 * oda kilidi (AuctionOrchestrator) bu metotlar çağrılırken tutulur.
 */
@Service
public class AuctionPersistence {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final UserSquadRepository userSquadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final CardRepository cardRepository;

    public AuctionPersistence(RoomRepository roomRepository,
                              RoomPlayerRepository roomPlayerRepository,
                              AuctionItemRepository auctionItemRepository,
                              UserSquadRepository userSquadRepository,
                              SquadPlayerRepository squadPlayerRepository,
                              CardRepository cardRepository) {
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.auctionItemRepository = auctionItemRepository;
        this.userSquadRepository = userSquadRepository;
        this.squadPlayerRepository = squadPlayerRepository;
        this.cardRepository = cardRepository;
    }

    /** Odayı SETUP'tan AUCTION_ACTIVE'e geçirir, her oyuncuya boş kadro açar, oyun görüntüsünü döner. */
    @Transactional
    public GameSnapshot beginGame(Long roomId, Long userId) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Oda bulunamadı"));
        roomPlayerRepository.findByRoom_IdAndUser_Id(roomId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NOT_IN_ROOM", "Bu odanın oyuncusu değilsin"));
        if (room.getStatus() != RoomStatus.SETUP) {
            throw new ApiException(HttpStatus.CONFLICT, "ROOM_NOT_IN_SETUP",
                    "Oyun başlatılamaz: oda SETUP durumunda değil (" + room.getStatus() + ")");
        }
        room.setStatus(RoomStatus.AUCTION_ACTIVE);

        List<GameSnapshot.PlayerSnapshot> players = new ArrayList<>();
        for (RoomPlayer rp : roomPlayerRepository.findByRoom_IdOrderBySeatNumber(roomId)) {
            if (userSquadRepository.findByRoomPlayer_Id(rp.getId()).isEmpty()) {
                userSquadRepository.save(new UserSquad(rp));
            }
            players.add(new GameSnapshot.PlayerSnapshot(rp.getId(), rp.getUser().getId(), rp.getSeatNumber(),
                    rp.getUser().getUsername(), rp.getCurrentBudget()));
        }

        List<GameSnapshot.ItemSnapshot> items = new ArrayList<>();
        for (AuctionItem item : auctionItemRepository.findByRoom_IdOrderBySequenceNo(roomId)) {
            if (item.getTier() == Tier.T3) {
                continue; // tier 3 ihaleye çıkmaz
            }
            Card card = item.getCard();
            Footballer footballer = item.getFootballer();
            items.add(new GameSnapshot.ItemSnapshot(item.getId(), item.getSequenceNo(), item.getSlot(),
                    item.getTier(), card.getId(), footballer.getPlayerName(), footballer.getNationality(),
                    footballer.getPhotoUrl(), card.getClub(), card.getPosition(), card.getStartingPrice()));
        }
        return new GameSnapshot(roomId, players, items);
    }

    @Transactional
    public void markItemActive(Long itemId) {
        AuctionItem item = auctionItemRepository.findById(itemId).orElseThrow();
        item.setStatus(AuctionStatus.ACTIVE);
        item.getRoom().setStatus(RoomStatus.AUCTION_ACTIVE);
    }

    /** Kimse ihaleyi açamadı: kart satılmaz. */
    @Transactional
    public void markItemUnsold(Long itemId) {
        AuctionItem item = auctionItemRepository.findById(itemId).orElseThrow();
        item.setStatus(AuctionStatus.UNSOLD);
        item.getRoom().setStatus(RoomStatus.AUCTION_CLOSED);
    }

    /** İhale kapandı: kart satıldı, bütçe düştü, kart kadroya girdi. */
    @Transactional
    public void recordSale(Long itemId, Long winnerRoomPlayerId, int price) {
        AuctionItem item = auctionItemRepository.findById(itemId).orElseThrow();
        RoomPlayer winner = roomPlayerRepository.findById(winnerRoomPlayerId).orElseThrow();

        item.setStatus(AuctionStatus.SOLD);
        item.setSoldPrice(price);
        item.setWinningRoomPlayer(winner);
        item.getRoom().setStatus(RoomStatus.AUCTION_CLOSED);

        winner.setCurrentBudget(winner.getCurrentBudget() - price);

        UserSquad squad = userSquadRepository.findByRoomPlayer_Id(winnerRoomPlayerId).orElseThrow();
        squad.setTotalSpent(squad.getTotalSpent() + price);
        squadPlayerRepository.save(
                new SquadPlayer(squad, item.getCard(), item.getSlot(), price, AcquisitionType.AUCTION));
    }

    /**
     * Tier 3 aşaması. Motor (Tier3Assigner) kimin hangi slotu hangi fiyata aldığını zaten
     * hesapladı; burada kartlar seçilip kadrolara yazılır, bütçeler düşer.
     * TIER3_AUTO: slotun tier 3 kartı verilir. U19_PENALTY: konseptin o mevkideki U19 havuzundan
     * rastgele kart, fiyat 0 (aynı futbolcu aynı kadroda tekrar etmez).
     */
    @Transactional
    public List<AuctionPayloads.Tier3Entry> assignTier3(Long roomId, Map<Slot, List<Tier3Assignment>> bySlot) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Oda bulunamadı"));
        room.setStatus(RoomStatus.TIER3_ASSIGN);
        Random random = new Random(room.getSeed() * 31 + 17);

        Map<Long, RoomPlayer> players = new HashMap<>();
        Map<Long, UserSquad> squads = new HashMap<>();
        Map<Long, Set<Long>> ownedFootballers = new HashMap<>();
        for (RoomPlayer rp : roomPlayerRepository.findByRoom_IdOrderBySeatNumber(roomId)) {
            players.put(rp.getId(), rp);
            UserSquad squad = userSquadRepository.findByRoomPlayer_Id(rp.getId()).orElseThrow();
            squads.put(rp.getId(), squad);
            Set<Long> owned = new HashSet<>();
            for (SquadPlayer sp : squadPlayerRepository.findBySquad_Id(squad.getId())) {
                owned.add(sp.getCard().getFootballer().getId());
            }
            ownedFootballers.put(rp.getId(), owned);
        }

        Long conceptId = room.getConcept().getId();
        Set<Long> usedU19Cards = new HashSet<>();
        List<AuctionPayloads.Tier3Entry> entries = new ArrayList<>();

        for (Slot slot : Slot.values()) {
            List<Tier3Assignment> assignments = bySlot.get(slot);
            if (assignments == null || assignments.isEmpty()) {
                continue;
            }
            AuctionItem t3 = auctionItemRepository.findByRoom_IdAndSlotAndTier(roomId, slot, Tier.T3)
                    .orElseThrow();
            List<Card> u19Pool = cardRepository
                    .findByConcept_IdAndPositionAndPenaltyCardTrue(conceptId, slot.getPosition()).stream()
                    .sorted(Comparator.comparing(Card::getId))
                    .toList();
            boolean anyTier3 = false;
            int tier3Price = 0;

            for (Tier3Assignment a : assignments) {
                RoomPlayer rp = players.get(a.playerId());
                UserSquad squad = squads.get(a.playerId());
                Set<Long> owned = ownedFootballers.get(a.playerId());
                Card card;
                if (a.type() == AcquisitionType.TIER3_AUTO) {
                    card = t3.getCard();
                    rp.setCurrentBudget(rp.getCurrentBudget() - a.price());
                    squad.setTotalSpent(squad.getTotalSpent() + a.price());
                    anyTier3 = true;
                    tier3Price = a.price();
                } else {
                    List<U19Picker.Candidate> candidates = u19Pool.stream()
                            .map(c -> new U19Picker.Candidate(c.getId(), c.getFootballer().getId()))
                            .toList();
                    U19Picker.Candidate chosen = U19Picker.pick(candidates, owned, usedU19Cards, random);
                    card = u19Pool.stream().filter(c -> c.getId() == chosen.cardId()).findFirst().orElseThrow();
                    usedU19Cards.add(card.getId());
                }
                owned.add(card.getFootballer().getId());
                squadPlayerRepository.save(new SquadPlayer(squad, card, slot, a.price(), a.type()));
                entries.add(new AuctionPayloads.Tier3Entry(rp.getId(), slot, a.type(), a.price(),
                        new AuctionPayloads.CardInfo(card.getId(), card.getFootballer().getPlayerName(),
                                card.getFootballer().getNationality(), card.getFootballer().getPhotoUrl(),
                                card.getClub(), card.getPosition(), card.getStartingPrice())));
            }
            // Kimse tier 3 kartını almadıysa (hepsi U19) kart kullanılmamış sayılır.
            t3.setStatus(anyTier3 ? AuctionStatus.AUTO_ASSIGNED : AuctionStatus.UNSOLD);
            if (anyTier3) {
                t3.setSoldPrice(tier3Price);
            }
        }
        return entries;
    }
}