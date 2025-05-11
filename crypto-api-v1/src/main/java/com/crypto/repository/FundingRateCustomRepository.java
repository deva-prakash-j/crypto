package com.crypto.repository;

import java.util.List;

import com.crypto.entity.FundingRate;

public interface FundingRateCustomRepository {
    void bulkInsertIgnoreConflicts(List<FundingRate> dataList);
}
