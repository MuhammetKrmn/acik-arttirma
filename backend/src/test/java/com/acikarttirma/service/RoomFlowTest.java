package com.acikarttirma.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acikarttirma.dto.RoomView;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.AuctionItem;
import com.acikarttirma.model.enums.RoomStatus;
import com.acikarttirma.model.enums.Tier;
import com.acikarttirma.repository.AuctionItemRepository;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** F4: oda kurma, katılma, 33 kart çekimi ve ihale sırası. Her test sonunda geri alınır. */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class RoomFlowTest {

    @Autowired
    private GuestService guestService;
    @Autowired
    private RoomService roomService;
    @Autowired
    private AuctionItemRepository auctionItemRepository;

    private String yeniMisafir() {
        return guestService.login("t" + UUID.randomUUID().toString().substring(0, 8)).playerToken();
    }

    private RoomView doluOda(long seed) {
        RoomView oda = roomService.createRoom(yeniMisafir(), seed);
        roomService.joinRoom(yeniMisafir(), oda.roomId());
        return roomService.joinRoom(yeniMisafir(), oda.roomId());
    }

    @Test
    void ikiOyuncuyaLobbyKalirVeKartCekilmez() {
        RoomView oda = roomService.createRoom(yeniMisafir(), 1L);
        RoomView sonra = roomService.joinRoom(yeniMisafir(), oda.roomId());

        assertEquals(RoomStatus.LOBBY, sonra.status());
        assertEquals(2, sonra.players().size());
        assertEquals(0, auctionItemRepository.findByRoom_IdOrderBySequenceNo(oda.roomId()).size());
    }

    @Test
    void ucuncuOyuncuGirinceSetupVe33KartCekilir() {
        RoomView oda = doluOda(42L);

        assertEquals(RoomStatus.SETUP, oda.status());
        assertEquals(3, oda.players().size());

        List<AuctionItem> items = auctionItemRepository.findByRoom_IdOrderBySequenceNo(oda.roomId());
        assertEquals(33, items.size());

        for (int i = 0; i < 22; i++) {
            assertEquals(i + 1, items.get(i).getSequenceNo().intValue());
            assertNotEquals(Tier.T3, items.get(i).getTier());
        }
        for (int i = 22; i < 33; i++) {
            assertNull(items.get(i).getSequenceNo());
            assertEquals(Tier.T3, items.get(i).getTier());
        }
        assertEquals(33, items.stream().map(i -> i.getSlot() + "-" + i.getTier()).distinct().count());
        assertEquals(33, items.stream().map(i -> i.getFootballer().getId()).distinct().count());
    }

    @Test
    void ayniSeedAyniOdayiUretir() {
        RoomView a = doluOda(7L);
        RoomView b = doluOda(7L);

        assertEquals(a.conceptName(), b.conceptName());

        List<AuctionItem> itemsA = auctionItemRepository.findByRoom_IdOrderBySequenceNo(a.roomId());
        List<AuctionItem> itemsB = auctionItemRepository.findByRoom_IdOrderBySequenceNo(b.roomId());

        List<Long> sıraA = itemsA.stream().filter(i -> i.getTier() != Tier.T3).map(i -> i.getCard().getId()).toList();
        List<Long> sıraB = itemsB.stream().filter(i -> i.getTier() != Tier.T3).map(i -> i.getCard().getId()).toList();
        assertEquals(sıraA, sıraB);

        assertEquals(
                new HashSet<>(itemsA.stream().map(i -> i.getCard().getId()).toList()),
                new HashSet<>(itemsB.stream().map(i -> i.getCard().getId()).toList()));
    }

    @Test
    void hataDurumlari() {
        String t1 = yeniMisafir();
        RoomView oda = roomService.createRoom(t1, 3L);

        ApiException zaten = assertThrows(ApiException.class, () -> roomService.joinRoom(t1, oda.roomId()));
        assertEquals("ALREADY_IN_ROOM", zaten.getCode());

        ApiException tokenYok = assertThrows(ApiException.class, () -> roomService.createRoom(null, null));
        assertEquals("TOKEN_REQUIRED", tokenYok.getCode());

        roomService.joinRoom(yeniMisafir(), oda.roomId());
        roomService.joinRoom(yeniMisafir(), oda.roomId());

        ApiException kapali = assertThrows(ApiException.class,
                () -> roomService.joinRoom(yeniMisafir(), oda.roomId()));
        assertEquals("ROOM_NOT_IN_LOBBY", kapali.getCode());
    }
}