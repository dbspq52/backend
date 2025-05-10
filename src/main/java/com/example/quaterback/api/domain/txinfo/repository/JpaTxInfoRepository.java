package com.example.quaterback.api.domain.txinfo.repository;

import com.example.quaterback.api.domain.charger.entity.ChargerEntity;
import com.example.quaterback.api.domain.charger.repository.SpringDataJpaChargerRepository;
import com.example.quaterback.api.domain.txinfo.domain.TransactionInfoDomain;
import com.example.quaterback.api.domain.txinfo.entity.TransactionInfoEntity;
import com.example.quaterback.api.feature.dashboard.dto.query.ChargerUsageQuery;
import com.example.quaterback.api.feature.dashboard.dto.query.DashboardSummaryQuery;
import com.example.quaterback.api.feature.dashboard.dto.response.ChargerUsageResponse;
import com.example.quaterback.api.feature.monitoring.dto.query.ChargingRecordQuery;
import com.example.quaterback.api.feature.monitoring.dto.query.DailyUsageQuery;
import com.example.quaterback.api.feature.monitoring.dto.query.HourlyCongestionQuery;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class JpaTxInfoRepository implements TxInfoRepository {

    private final SpringDataJpaTxInfoRepository springDataJpaTxInfoRepository;
    private final SpringDataJpaChargerRepository springDataJpaChargerRepository;

    @Override
    public String save(TransactionInfoDomain domain) {
        String stationId = domain.getStationId();
        Integer evseId = domain.getEvseId();

        ChargerEntity chargerEntity = springDataJpaChargerRepository.findByStation_StationIdAndEvseId(stationId, evseId)
                .orElseThrow(() -> new EntityNotFoundException("entity not found"));

        TransactionInfoEntity entity = TransactionInfoEntity.fromTransactionInfoDomain(domain, chargerEntity);
        springDataJpaTxInfoRepository.save(entity);
        return entity.getTransactionId();
    }

    @Override
    public String updateEndTime(TransactionInfoDomain domain) {
        TransactionInfoEntity entity = springDataJpaTxInfoRepository.findByTransactionId(domain.getTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("tx info entity not found"));

        if (domain.getEndedTime() != null && domain.getTotalMeterValue() != null && domain.getTotalPrice() != null) {
            String returnTxId = entity.updateEndTimeAndTotalValues(domain);
            return returnTxId;
        }
        return null;
    }

    @Override
    public List<Object[]> findTotalDischargePerHour() {
        return springDataJpaTxInfoRepository.findTotalDischargePerHour();
    }

    @Override
    public DashboardSummaryQuery findDashboardSummary() {
        return springDataJpaTxInfoRepository.findDashboardSummary();
    }

    @Override
    public List<ChargerUsageQuery> findWithStationInfo() {
        return springDataJpaTxInfoRepository.findWithStationInfo();
    }

    @Override
    public Page<ChargingRecordQuery> findChargerUsageByStationId(String stationId, Pageable pageable) {
        return springDataJpaTxInfoRepository.findChargerUsageByStationId(stationId, pageable);
    }

    @Override
    public List<HourlyCongestionQuery> findHourlyCountsByStationId(String stationId) {
        return springDataJpaTxInfoRepository.findHourlyCountsByStationId(stationId);
    }

    @Override
    public Page<TransactionInfoDomain> findAllByEvseId(String stationId, Integer evseId, Pageable pageable) {
        ChargerEntity chargerEntity = springDataJpaChargerRepository.findByStation_StationIdAndEvseId(stationId, evseId)
                .orElseThrow(() -> new EntityNotFoundException("entity not found"));

        Page<TransactionInfoEntity> entityPage = springDataJpaTxInfoRepository.findAllByEvseId(stationId, chargerEntity.getId(), pageable);

        return entityPage.map(TransactionInfoEntity::toDomain);
    }

    @Override
    public DailyUsageQuery findDailyUsageByEvseIdAndDate(String stationId, Integer evseId, LocalDate date) {
        ChargerEntity chargerEntity = springDataJpaChargerRepository.findByStation_StationIdAndEvseId(stationId, evseId)
                .orElseThrow(() -> new EntityNotFoundException("entity not found"));

        return springDataJpaTxInfoRepository.findDailyUsageByEvseIdAndDate(stationId, chargerEntity.getId(), date)
                .orElseThrow(() -> new EntityNotFoundException("tx info entity not found"));
    }

    @Override
    public Page<TransactionInfoDomain> findByIdTokenOrderByStartedTimeDesc(String idToken, Pageable pageable) {
        Page<TransactionInfoEntity> entityPage = springDataJpaTxInfoRepository.findByIdTokenOrderByStartedTimeDesc(idToken, pageable);
        return entityPage.map(TransactionInfoEntity::toDomain);
    }

    @Override
    public Page<TransactionInfoDomain> findByIdTokenAndStartedTimeBetweenOrderByStartedTimeDesc(String idToken, LocalDate startTime, LocalDate endTime, Pageable pageable) {
        Page<TransactionInfoEntity> entityPage = springDataJpaTxInfoRepository.findByIdTokenAndStartedTimeBetweenOrderByStartedTimeDesc(idToken, startTime, endTime, pageable);
        return entityPage.map(TransactionInfoEntity::toDomain);
    }


    @Override
    public List<TransactionInfoDomain> findByChargerPkAndCreatedAtBetween(TransactionInfoDomain domain,
                                                                          Long chargerPk) {
        LocalDateTime start = domain.getStartedTime();
        LocalDateTime end = domain.getEndedTime();

        List<TransactionInfoEntity> txEntities = springDataJpaTxInfoRepository.findByChargerPkAndTimeRange(
                chargerPk
                ,start
                ,end);
        List<TransactionInfoDomain> txDomains = txEntities.stream()
                .map(TransactionInfoDomain::fromTxEntityDomain)
                .collect(Collectors.toList());
        return txDomains;
    }

    @Override
    public Page<TransactionInfoDomain> findByStationIdAndCreatedAtBetween(TransactionInfoDomain domain,
                                                                          String stationId,
                                                                          Pageable pageable) {
        LocalDateTime start = domain.getStartedTime();
        LocalDateTime end = domain.getEndedTime();

        Page<TransactionInfoEntity> txInfoEntities =  springDataJpaTxInfoRepository.findByStationIdAndPeriod(
                stationId, start, end, pageable
        );

        Page<TransactionInfoDomain> txInfoDomains = txInfoEntities
                .map(entity -> TransactionInfoDomain.fromTxEntityDomain(entity));
        return txInfoDomains;
    }

    @Override
    public TransactionInfoDomain getOneTxInfoByTxId(TransactionInfoDomain txInfoDomain) {
        TransactionInfoEntity txEntity = springDataJpaTxInfoRepository.findByTransactionId(txInfoDomain.getTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("tx info entity not found"));
        return TransactionInfoDomain.fromTxEntityDomain(txEntity);
    }


}
