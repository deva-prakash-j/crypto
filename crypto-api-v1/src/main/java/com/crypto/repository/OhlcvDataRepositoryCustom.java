package com.crypto.repository;

import java.util.List;

import com.crypto.entity.OhlcvData;

public interface OhlcvDataRepositoryCustom {
    void bulkInsertIgnoreConflicts(List<OhlcvData> dataList);
}

