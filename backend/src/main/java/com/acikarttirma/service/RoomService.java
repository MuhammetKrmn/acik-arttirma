package com.acikarttirma.service;

import com.acikarttirma.dto.RoomView;
import com.acikarttirma.exception.ApiException;
import com.acikarttirma.model.Concept;
import com.acikarttirma.model.Room;
import com.acikarttirma.model.RoomPlayer;
import com.acikarttirma.model.User;
import com.acikarttirma.model.enums.RoomStatus;
import com.acikarttirma.repository.ConceptRepository;
import com.acikarttirma.repository.RoomPlayerRepository;
import com.acikarttirma.repository.RoomRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    public static final int STARTING_BUDGET = 1000;
    public static final int ROOM_SIZE = 3;

    private final GuestService guestService;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptPoolChecker poolChecker;
    private final RoomCardDrawer cardDrawer;

    public RoomService(GuestService guestService,
                       RoomRepository roomRepository,
                       RoomPlayerRepository roomPlayerRepository,
                       ConceptRepository conceptRepository,
                       ConceptPoolChecker poolChecker,
                       RoomCardDrawer cardDrawer) {
        this.guestService = guestService;
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.conceptRepository = conceptRepository;
        this.poolChecker = poolChecker;
        this.cardDrawer = cardDrawer;
    }

    /** Odayı kurar, konsepti seed ile rastgele seçer, kuran kişi 1. koltuğa oturur. */
    @Transactional
    public RoomView createRoom(String token, Long requestedSeed) {
        User user = guestService.authenticate(token);
        long seed = requestedSeed != null ? requestedSeed : ThreadLocalRandom.current().nextLong();
        Concept concept = pickConcept(seed);
        Room room = roomRepository.save(new Room(concept, STARTING_BUDGET, seed));
        roomPlayerRepository.save(new RoomPlayer(room, user, (short) 1, STARTING_BUDGET));
        return toView(room);
    }

    /** 3. oyuncu girince oda SETUP'a geçer ve 33 kart çekilir. */
    @Transactional
    public RoomView joinRoom(String token, Long roomId) {
        User user = guestService.authenticate(token);
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Oda bulunamadı"));

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new ApiException(HttpStatus.CONFLICT, "ROOM_NOT_IN_LOBBY", "Oda artık katılıma açık değil");
        }
        if (roomPlayerRepository.findByRoom_IdAndUser_Id(roomId, user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_IN_ROOM", "Zaten bu odadasın");
        }
        int count = (int) roomPlayerRepository.countByRoom_Id(roomId);
        if (count >= ROOM_SIZE) {
            throw new ApiException(HttpStatus.CONFLICT, "ROOM_FULL", "Oda dolu");
        }

        roomPlayerRepository.save(new RoomPlayer(room, user, (short) (count + 1), room.getStartingBudget()));

        if (count + 1 == ROOM_SIZE) {
            room.setStatus(RoomStatus.SETUP);
            cardDrawer.draw(room);
        }
        return toView(room);
    }

    @Transactional(readOnly = true)
    public RoomView getRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "Oda bulunamadı"));
        return toView(room);
    }

    /** Katılıma açık (LOBBY) odalar. */
    @Transactional(readOnly = true)
    public List<RoomView> listOpenRooms() {
        return roomRepository.findByStatus(RoomStatus.LOBBY).stream().map(this::toView).toList();
    }

    private Concept pickConcept(long seed) {
        List<Concept> eligible = conceptRepository.findByActiveTrue().stream()
                .filter(c -> poolChecker.isComplete(c.getId()))
                .sorted(Comparator.comparing(Concept::getId))
                .toList();
        if (eligible.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "NO_PLAYABLE_CONCEPT",
                    "Oyuna uygun konsept yok (kart havuzu eksik)");
        }
        return eligible.get(new Random(seed ^ 0x5DEECE66DL).nextInt(eligible.size()));
    }

    private RoomView toView(Room room) {
        List<RoomView.PlayerView> players = roomPlayerRepository.findByRoom_IdOrderBySeatNumber(room.getId())
                .stream()
                .map(p -> new RoomView.PlayerView(p.getId(), p.getUser().getId(), p.getSeatNumber(),
                        p.getUser().getUsername(), p.getCurrentBudget()))
                .toList();
        return new RoomView(room.getId(), room.getStatus(), room.getConcept().getConceptName(),
                room.getStartingBudget(), players);
    }
}