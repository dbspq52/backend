package com.example.quaterback.api.domain.txlog.repository;

import com.example.quaterback.api.domain.txlog.entity.TransactionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataJpaTxLogRepository extends JpaRepository<TransactionLogEntity, Long> {
    @Query("SELECT AVG(t.meterValue) FROM TransactionLogEntity t WHERE t.transactionId = :transactionId")
    Integer avgMeterValueByTransactionId(@Param("transactionId") String transactionId);
}
