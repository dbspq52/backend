package com.example.quaterback.websocket.transaction.event.service;

import com.example.quaterback.api.domain.charger.domain.ChargerDomain;
import com.example.quaterback.api.feature.dashboard.dto.query.ChargerUsageQuery;
import com.example.quaterback.api.feature.dashboard.dto.query.DashboardSummaryQuery;
import com.example.quaterback.api.feature.dashboard.dto.response.ChargerUsageResponse;
import com.example.quaterback.api.feature.dashboard.dto.response.DashboardSummaryResponse;
import com.example.quaterback.api.feature.dashboard.dto.response.HourlyDischargeResponse;
import com.example.quaterback.api.feature.monitoring.dto.query.ChargingRecordQuery;
import com.example.quaterback.api.feature.monitoring.dto.query.DailyUsageQuery;
import com.example.quaterback.api.feature.monitoring.dto.query.HourlyCongestionQuery;
import com.example.quaterback.api.feature.monitoring.dto.response.ChargingRecordResponse;
import com.example.quaterback.api.feature.monitoring.dto.response.ChargingRecordResponsePage;
import com.example.quaterback.api.feature.monitoring.dto.response.DailyUsageDto;
import com.example.quaterback.api.feature.monitoring.dto.response.HourlyCongestion;
import com.example.quaterback.common.redis.service.RedisMapSessionToStationService;
import com.example.quaterback.api.domain.txinfo.domain.TransactionInfoDomain;
import com.example.quaterback.api.domain.txinfo.repository.TxInfoRepository;
import com.example.quaterback.api.domain.txlog.domain.TransactionLogDomain;
import com.example.quaterback.api.domain.txlog.repository.TxLogRepository;
import com.example.quaterback.websocket.transaction.event.converter.TransactionEventConverter;
import com.example.quaterback.websocket.transaction.event.domain.TransactionEventDomain;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventService {

    private final TransactionEventConverter converter;
    private final TxInfoRepository txInfoRepository;
    private final TxLogRepository txLogRepository;
    private final RedisMapSessionToStationService redisMappingService;

    public String saveTxInfo(JsonNode jsonNode, String sessionId) {
        TransactionEventDomain txEventDomain = converter.convertToStartedTransactionDomain(jsonNode);

        String stationId = redisMappingService.getStationId(sessionId);
        TransactionInfoDomain txInfoDomain = TransactionInfoDomain.fromStartedTxEventDomain(txEventDomain, stationId);

        String resultTransactionId = txInfoRepository.save(txInfoDomain);
        return resultTransactionId;
    }

    public String saveTxLog(JsonNode jsonNode) {
        TransactionEventDomain txEventDomain = converter.convertToNonStartedTransactionDomain(jsonNode);

        TransactionLogDomain txLogDomain = TransactionLogDomain.fromTxEventDomain(txEventDomain);

        String resultTransactionId = txLogRepository.save(txLogDomain);
        return resultTransactionId;
    }

    @Transactional
    public String updateTxEndTime(JsonNode jsonNode) {
        saveTxLog(jsonNode);

        TransactionEventDomain txEventDomain = converter.convertToNonStartedTransactionDomain(jsonNode);

        int avgMeterValue = txLogRepository.getTotalMeterValue(txEventDomain.extractTransactionId());
        int pricePerWh = 100;

        TransactionInfoDomain oriTxInfoDomain = txInfoRepository.findByTxId(txEventDomain.extractTransactionId());

        int durationSeconds = (int) Duration.between(oriTxInfoDomain.getStartedTime(), txEventDomain.getTimestamp()).getSeconds();
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 이후여야합니다.");
        }

        double totalMeterValue = avgMeterValue / 1000.0 * (durationSeconds / 3600.0);
        totalMeterValue = Math.round(totalMeterValue * 100) / 100.0;
        Double totalPrice = pricePerWh * totalMeterValue;

        TransactionInfoDomain endTxInfoDomain = TransactionInfoDomain.fromEndedTxEventDomain(txEventDomain, totalMeterValue, totalPrice);
        String resultTransactionId = txInfoRepository.updateEndTime(endTxInfoDomain);

        return resultTransactionId;
    }

    @Transactional
    public List<HourlyDischargeResponse> getHourlyDischarge() {
        List<Object[]> result = txInfoRepository.findTotalDischargePerHour();
        return converter.toHourlyDischargeResponseList(result);
    }

    public DashboardSummaryResponse getDailySummary() {
        DashboardSummaryQuery query = txInfoRepository.findDashboardSummary();
        return converter.toDashboardSummaryResponse(query);
    }

    public List<ChargerUsageResponse> getChargerUsage() {
        List<ChargerUsageQuery> queryList = txInfoRepository.findWithStationInfo();
        return converter.toChargerUsageResponseList(queryList);
    }

    public Page<ChargingRecordQuery> findChargingEventsByStationId(String stationId, Pageable pageable) {
        return txInfoRepository.findChargerUsageByStationId(stationId, pageable);
    }

    public List<HourlyCongestion> findHourlyCount(String stationId) {
        List<HourlyCongestionQuery> queryList = txInfoRepository.findHourlyCountsByStationId(stationId);
        return converter.toHourlyCongestionList(queryList);
    }

    public Page<TransactionInfoDomain> findTransactionInfo(String stationId, Integer evseId, Pageable pageable) {
        return txInfoRepository.findAllByEvseId(stationId, evseId, pageable);
    }

    public DailyUsageQuery findOneDayUsageInfo(String stationId, Integer evseId, LocalDate date) {
        return txInfoRepository.findDailyUsageByEvseIdAndDate( stationId, evseId, date);
    }
}
