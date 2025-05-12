package com.crypto.repository;

import java.util.List;

import com.crypto.entity.LongShortRatio;

public interface LongShortRatioCustomRepository {
    void bulkInsertIgnoreConflicts(List<LongShortRatio> dataList);
}
