package com.crypto.repository;

import java.util.List;

import com.crypto.entity.OpenInterest;

public interface OpenInterestCustomRepository {
     void bulkInsertIgnoreConflicts(List<OpenInterest> dataList);
}
