package com.acikarttirma.service;

import com.acikarttirma.dto.GameResultView;
import com.acikarttirma.engine.GameResult;
import com.acikarttirma.engine.PlayerScore;
import com.acikarttirma.engine.Scoring;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.Card;
import com.acikarttirma.model.Room;
import com.acikarttirma.model.RoomPlayer;
import com.acikarttirma.model.SquadPlayer;
import com.acikarttirma.model.UserSquad;
import com.acikarttirma.model.enums.RoomStatus;
import com.acikarttirma.repository.RoomPlayerRepository;
import com.acikarttirma.repository.RoomRepository;
import com.acikarttirma.repository.SquadPlayerRepository;
import com.acikarttirma.repository.UserSquadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Oyun sonu: kadro reytinglerini toplar, kazananı belirler (reyting, sonra kalan bütçe,
 * sonra beraberlik), odayı FINISHED yapar. Sonuç DB'den okunur, yani sunucu yeniden
 * başlasa da /result çalışır.
 */
@Service
public class ResultService {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final UserSquadRepository userSquadRepository;
    private final SquadPlayerRepository squadPlayerRepository;

    public ResultService(RoomRepository roomRepository,
                         RoomPlayerRepository roomPlayerRepository,
                         UserSquadRepository userSquadRepository,
                         SquadPlayerRepository squadPlayerRepository) {
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.userSquadRepository = userSquadRepository;
        this.squadPlayerRepository = squadPlayerRepository;
    }

    /** Skorları yazar, kazananı belirler, odayı FINISHED yapar ve sonucu döner. */
    @Transactional
    public GameResultView finishGame(Long roomId) {
        Room room = roomRepository.findByIdForUpdate(roomId).orElseThrow(this::roomNotFound);
        List<RoomPlayer> players = roomPlayerRepository.findByRoom_IdOrderBySeatNumber(roomId);
        for (RoomPlayer rp : players) {
            UserSquad squad = squadOf(rp);
            int total = squadPlayerRepository.findBySquad_Id(squad.getId()).stream()
                    .mapToInt(sp -> sp.getCard().getOverallRating())
                    .sum();
            squad.setTotalRating(total);
        }
        GameResultView view = buildView(room, players);
        room.setStatus(RoomStatus.FINISHED);
        room.setFinishedAt(Instant.now());
        room.setWinnerRoomPlayer(view.draw() ? null : players.stream()
                .filter(rp -> rp.getId().equals(view.winnerRoomPlayerIds().get(0)))
                .findFirst().orElseThrow());
        return view;
    }

    /** Bitmiş oyunun sonucu. Oda FINISHED değilse 409. */
    @Transactional(readOnly = true)
    public GameResultView getResult(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(this::roomNotFound);
        if (room.getStatus() != RoomStatus.FINISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "GAME_NOT_FINISHED", "Oyun henüz bitmedi");
        }
        return buildView(room, roomPlayerRepository.findByRoom_IdOrderBySeatNumber(roomId));
    }

    private GameResultView buildView(Room room, List<RoomPlayer> players) {
        List<PlayerScore> scores = new ArrayList<>();
        for (RoomPlayer rp : players) {
            scores.add(new PlayerScore(rp.getId(), squadOf(rp).getTotalRating(), rp.getCurrentBudget()));
        }
        GameResult result = Scoring.decide(scores);

        List<GameResultView.PlayerResult> views = new ArrayList<>();
        for (RoomPlayer rp : players) {
            UserSquad squad = squadOf(rp);
            List<GameResultView.SquadEntry> entries = squadPlayerRepository.findBySquad_Id(squad.getId()).stream()
                    .sorted(Comparator.comparing(SquadPlayer::getSlot))
                    .map(this::entry)
                    .toList();
            views.add(new GameResultView.PlayerResult(rp.getId(), rp.getUser().getId(), rp.getSeatNumber(),
                    rp.getUser().getUsername(), squad.getTotalRating(), rp.getCurrentBudget(),
                    squad.getTotalSpent(), result.winnerIds().contains(rp.getId()), entries));
        }
        return new GameResultView(room.getId(), result.isDraw(), result.winnerIds(), views);
    }

    private GameResultView.SquadEntry entry(SquadPlayer sp) {
        Card card = sp.getCard();
        return new GameResultView.SquadEntry(sp.getSlot(), card.getFootballer().getPlayerName(),
                card.getFootballer().getNationality(), card.getFootballer().getPhotoUrl(), card.getClub(),
                card.getPosition(), card.getOverallRating(), sp.getPaidPrice(), sp.getAcquisitionType());
    }

    private UserSquad squadOf(RoomPlayer rp) {
        return userSquadRepository.findByRoomPlayer_Id(rp.getId()).orElseThrow();
    }

    private ApiException roomNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Oda bulunamadı");
    }
}