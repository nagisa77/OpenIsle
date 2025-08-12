package com.openisle.controller;

import com.openisle.dto.MedalDto;
import com.openisle.dto.MedalSelectRequest;
import com.openisle.service.MedalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medals")
@RequiredArgsConstructor
public class MedalController {
    private final MedalService medalService;

    @GetMapping
    public List<MedalDto> getMedals(@RequestParam(value = "userId", required = false) Long userId) {
        return medalService.getMedals(userId);
    }

    @PostMapping("/select")
    public ResponseEntity<Void> selectMedal(@RequestBody MedalSelectRequest req, Authentication auth) {
        try {
            medalService.selectMedal(auth.getName(), req.getType());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
