package com.acikarttirma.controller;

import com.acikarttirma.dto.GameResultView;
import com.acikarttirma.service.ResultService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /** Bitmiş oyunun sonucu: skorlar, kadrolar, kazanan. Oyun bitmediyse 409 GAME_NOT_FINISHED. */
    @GetMapping("/{roomId}/result")
    public GameResultView result(@PathVariable Long roomId) {
        return resultService.getResult(roomId);
    }
}