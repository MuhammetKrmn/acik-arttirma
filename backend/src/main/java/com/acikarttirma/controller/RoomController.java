package com.acikarttirma.controller;

import com.acikarttirma.dto.CreateRoomRequest;
import com.acikarttirma.dto.RoomView;
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

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomView create(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                           @RequestBody(required = false) CreateRoomRequest request) {
        Long seed = request != null ? request.seed() : null;
        return roomService.createRoom(token, seed);
    }

    @PostMapping("/{roomId}/join")
    public RoomView join(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                         @PathVariable Long roomId) {
        return roomService.joinRoom(token, roomId);
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
}