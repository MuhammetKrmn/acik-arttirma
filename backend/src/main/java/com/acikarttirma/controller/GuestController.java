package com.acikarttirma.controller;

import com.acikarttirma.dto.GuestRequest;
import com.acikarttirma.dto.GuestResponse;
import com.acikarttirma.service.GuestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guests")
public class GuestController {

    private final GuestService guestService;

    public GuestController(GuestService guestService) {
        this.guestService = guestService;
    }

    /** Misafir girişi: kullanıcı adı ver, userId ve playerToken al. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuestResponse login(@Valid @RequestBody GuestRequest request) {
        return guestService.login(request.username());
    }
}