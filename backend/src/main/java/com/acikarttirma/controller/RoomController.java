package com.acikarttirma.controller;

import com.acikarttirma.dto.CreateRoomRequest;
import com.acikarttirma.dto.LiveStateView;
import com.acikarttirma.dto.LobbyView;
import com.acikarttirma.dto.ReadyRequest;
import com.acikarttirma.dto.RoomView;
import com.acikarttirma.service.AuctionOrchestrator;
import com.acikarttirma.service.LobbyService;
import com.acikarttirma.service.RoomService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final String TOKEN_HEADER = "X-Player-Token";

    private final RoomService roomService;
    private final AuctionOrchestrator auctionOrchestrator;
    private final LobbyService lobbyService;

    public RoomController(RoomService roomService, AuctionOrchestrator auctionOrchestrator,
                          LobbyService lobbyService) {
        this.roomService = roomService;
        this.auctionOrchestrator = auctionOrchestrator;
        this.lobbyService = lobbyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomView create(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                           @RequestBody(required = false) CreateRoomRequest request) {
        Long seed = request != null ? request.seed() : null;
        RoomView room = roomService.createRoom(token, seed);
        lobbyService.onRoomChanged(room);
        return room;
    }

    /** Tek tıkla eşleşme: açık odaya sokar, yoksa oda kurar. Frontend'in kullanacağı yol budur. */
    @PostMapping("/quick-join")
    public RoomView quickJoin(@RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        return lobbyService.quickJoin(token);
    }

    @PostMapping("/{roomId}/join")
    public RoomView join(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                         @PathVariable Long roomId) {
        RoomView room = roomService.joinRoom(token, roomId);
        lobbyService.onRoomChanged(room);
        return room;
    }

    @GetMapping("/{roomId}")
    public RoomView get(@PathVariable Long roomId) {
        return roomService.getRoom(roomId);
    }

    /** Katılıma açık odalar. */
    @GetMapping
    public List<RoomView> listOpen() {
        return roomService.listOpenRooms();
    }

    /** Lobinin anlık durumu: oyuncular, kim hazır, oyunun kendiliğinden başlayacağı an. */
    @GetMapping("/{roomId}/lobby")
    public LobbyView lobby(@PathVariable Long roomId) {
        return lobbyService.getLobby(roomId);
    }

    /** "Hazırım" (gövdesiz veya {"ready": true}) ve "İptal" ({"ready": false}). */
    @PostMapping("/{roomId}/ready")
    public LobbyView ready(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                           @PathVariable Long roomId,
                           @RequestBody(required = false) ReadyRequest request) {
        boolean ready = request == null || request.ready() == null || request.ready();
        return lobbyService.setReady(roomId, token, ready);
    }

    /** SETUP durumundaki odayı elle başlatır (test sayfası için). Normalde oyunu lobi başlatır. */
    @PostMapping("/{roomId}/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void start(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                      @PathVariable Long roomId) {
        auctionOrchestrator.startGame(roomId, token);
        lobbyService.onStartedElsewhere(roomId);
    }

    /** Canlı oyunun anlık durumu (istemci kendini senkronlamak için). */
    @GetMapping("/{roomId}/live")
    public LiveStateView live(@PathVariable Long roomId) {
        return auctionOrchestrator.getState(roomId);
    }
}