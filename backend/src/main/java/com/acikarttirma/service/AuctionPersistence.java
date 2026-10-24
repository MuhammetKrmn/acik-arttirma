package com.acikarttirma.service;

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
import com.acikarttirma.model.enums.Tier;
import com.acikarttirma.repository.AuctionItemRepository;
import com.acikarttirma.repository.RoomPlayerRepository;
import com.acikarttirma.repository.RoomRepository;
import com.acikarttirma.repository.SquadPlayerRepository;
import com.acikarttirma.repository.UserSquadRepository;
import java.util.ArrayList;
import java.util.List;
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

    public AuctionPersistence(RoomRepository roomRepository,
                              RoomPlayerRepository roomPlayerRepository,
                              AuctionItemRepository auctionItemRepository,
                              UserSquadRepository userSquadRepository,
                              SquadPlayerRepository squadPlayerRepository) {
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.auctionItemRepository = auctionItemRepository;
        this.userSquadRepository = userSquadRepository;
        this.squadPlayerRepository = squadPlayerRepository;
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
}