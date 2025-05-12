package com.crypto.service;

import java.util.Map;

import com.crypto.dto.SyncDTO;

public interface SyncService {
    Map<String, SyncDTO> getSyncStatus();
}
