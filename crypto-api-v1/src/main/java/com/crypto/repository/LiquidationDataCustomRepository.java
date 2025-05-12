package com.crypto.repository;

import java.util.List;

import com.crypto.entity.LiquidationData;

public interface LiquidationDataCustomRepository {
    void bulkInsertIgnoreConflicts(List<LiquidationData> dataList);
}
