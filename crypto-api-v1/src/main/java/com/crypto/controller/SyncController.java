package com.crypto.controller;

import org.springframework.web.bind.annotation.RestController;

import com.crypto.dto.SyncDTO;
import com.crypto.service.SyncService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {
    
    private final SyncService service;
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, SyncDTO>> getSyncStatus() {
        return ResponseEntity.ok().body(service.getSyncStatus());
    }
    
}
