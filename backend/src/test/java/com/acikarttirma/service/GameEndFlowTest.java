package com.acikarttirma.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acikarttirma.dto.AuctionEvent;
import com.acikarttirma.dto.AuctionPayloads;
import com.acikarttirma.dto.GameResultView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * F6: oyun sonu. Bir oyuncunun bütçesi 100'e düşürülür, böylece tier 3 ve U19 yolları da çalışır.
 * Süreler çok kısa: 22 ihale birkaç saniyede biter.
 */
@SpringBootTest(properties = {
        "auction.first-delay-ms=50",
        "auction.next-delay-ms=50",
        "auction.bid-timeout-ms=150"
})
@ActiveProfiles("dev")
@Import(FlowTestSupport.RecorderConfig.class)
class GameEndFlowTest extends FlowTestSupport {

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    @Test
    void oyunBitinceHerKadro11KartOlurSkorYazilirOdaBiter() throws Exception {
        Setup s = kur(21L);
        Long roomId = s.room().roomId();
        Long poorId = roomPlayerIdOf(s.room(), s.guests().get(2).userId());
        jdbc.update("UPDATE room_players SET current_budget = 100 WHERE room_player_id = ?", poorId);
        orchestrator.startGame(roomId, s.guests().get(0).playerToken());

        AuctionEvent finishedEvent = bekle(roomId, AuctionEvent.GAME_FINISHED, 60_000);

        // Olay sırası: ihaleler biter, tier 3 atanır, oyun biter.
        int auctionsFinished = indexOf(roomId, AuctionEvent.AUCTIONS_FINISHED);
        int tier3 = indexOf(roomId, AuctionEvent.TIER3_ASSIGNED);
        int gameFinished = indexOf(roomId, AuctionEvent.GAME_FINISHED);
        assertTrue(auctionsFinished >= 0 && auctionsFinished < tier3 && tier3 < gameFinished);

        AuctionPayloads.Tier3Assigned tier3Data = (AuctionPayloads.Tier3Assigned) recorder.all.get(tier3).event().data();
        GameResultView view = (GameResultView) finishedEvent.data();
        assertEquals(3, view.players().size());

        for (GameResultView.PlayerResult p : view.players()) {
            Long rp = p.roomPlayerId();
            String squadJoin = " FROM squad_players sp JOIN user_squads us ON us.squad_id = sp.squad_id "
                    + "JOIN cards c ON c.card_id = sp.card_id WHERE us.room_player_id = ?";

            // Her kadro tam 11 slot, aynı futbolcu iki kez yok.
            assertEquals(11, count("SELECT COUNT(*)" + squadJoin, rp));
            assertEquals(11, count("SELECT COUNT(DISTINCT c.footballer_id)" + squadJoin, rp));
            assertEquals(11, p.squad().size());

            // Bütçe tutarlı: başlangıç - ödenen toplam = kalan, hiç eksiye düşmedi.
            int start = rp.equals(poorId) ? 100 : 1000;
            int spent = count("SELECT COALESCE(SUM(sp.paid_price), 0)" + squadJoin, rp);
            int budget = count("SELECT current_budget FROM room_players WHERE room_player_id = ?", rp);
            assertEquals(start - spent, budget);
            assertTrue(budget >= 0);
            assertEquals(budget, p.remainingBudget());
            assertEquals(budget, tier3Data.budgets().get(rp).intValue());

            // Toplam reyting, kadrodaki kartların reytinglerinin toplamı.
            int ratingSum = p.squad().stream().mapToInt(GameResultView.SquadEntry::overallRating).sum();
            assertEquals(ratingSum, p.totalRating());
            assertEquals(ratingSum, count("SELECT total_rating FROM user_squads WHERE room_player_id = ?", rp));
        }

        // Bütçesi 100 olan oyuncu en az bir U19 ceza kartı almış olmalı (en fazla 5 kart parayla alınabilir).
        String poorU19 = "SELECT COUNT(*) FROM squad_players sp JOIN user_squads us ON us.squad_id = sp.squad_id "
                + "WHERE us.room_player_id = ? AND sp.acquisition_type = 'U19_PENALTY'";
        assertTrue(count(poorU19, poorId) > 0);

        // Tier 3 kartları artık beklemede değil.
        assertEquals(0, count("SELECT COUNT(*) FROM auction_items WHERE room_id = ? AND tier = 3 "
                + "AND status = 'PENDING'", roomId));

        // Oda bitti ve kazanan, en yüksek reytingli oyuncu (beraberlikte kalan bütçe, sonra beraberlik).
        String status = jdbc.queryForObject("SELECT status FROM rooms WHERE room_id = ?", String.class, roomId);
        assertEquals("FINISHED", status);
        GameResultView fromDb = resultService.getResult(roomId);
        assertEquals(view.winnerRoomPlayerIds(), fromDb.winnerRoomPlayerIds());
        long winners = view.players().stream().filter(GameResultView.PlayerResult::winner).count();
        assertEquals(view.winnerRoomPlayerIds().size(), (int) winners);
        int best = view.players().stream().mapToInt(GameResultView.PlayerResult::totalRating).max().orElseThrow();
        for (GameResultView.PlayerResult p : view.players()) {
            if (p.winner()) {
                assertEquals(best, p.totalRating());
            }
        }
        if (!view.draw()) {
            assertEquals(1, view.winnerRoomPlayerIds().size());
            assertFalse(view.players().stream().filter(p -> !p.winner())
                    .anyMatch(p -> p.totalRating() > best));
        }
    }
}