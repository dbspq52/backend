package com.example.quaterback.api.domain.txinfo.service;

import com.example.quaterback.api.domain.charger.domain.ChargerDomain;
import com.example.quaterback.api.domain.charger.entity.ChargerEntity;
import com.example.quaterback.api.domain.charger.repository.ChargerRepository;
import com.example.quaterback.api.domain.charger.repository.SpringDataJpaChargerRepository;
import com.example.quaterback.api.domain.station.repository.ChargingStationRepository;
import com.example.quaterback.api.domain.txinfo.domain.TransactionInfoDomain;
import com.example.quaterback.api.domain.txinfo.entity.TransactionInfoEntity;
import com.example.quaterback.api.domain.txinfo.repository.JpaTxInfoRepository;
import com.example.quaterback.api.domain.txinfo.repository.SpringDataJpaTxInfoRepository;
import com.example.quaterback.api.domain.txinfo.repository.TxInfoRepository;
import com.example.quaterback.api.feature.managing.dto.apiRequest.CreateTransactionInfoRequest;
import com.example.quaterback.api.feature.managing.dto.txInfo.TransactionInfoDto;
import com.example.quaterback.api.feature.managing.dto.txInfo.TransactionSummaryDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionInfoService {
    private final TxInfoRepository txInfoRepository;
    private final ChargerRepository chargerRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final SpringDataJpaTxInfoRepository springDataJpaTxInfoRepository;
    private final SpringDataJpaChargerRepository springDataJpaChargerRepository;
    private final JpaTxInfoRepository jpaTxInfoRepository;
    //charger 별 충전기록 얻기
    public TransactionSummaryDto getChargerTransactionsByStationAndPeriod(LocalDateTime start,
                                                                                LocalDateTime end,
                                                                                String stationName){

        TransactionInfoDomain domain = TransactionInfoDomain.fromLocalDateTimeToDomain(
                start
                ,end
        );
        //stationId 얻고
        String stationId = chargingStationRepository.findStationIdByStationName(stationName);

        //stationId로 충전소에 관련된 Charger 객체들 뽑고
        List<ChargerDomain> chargerList = new ArrayList<>(chargerRepository.findByStationID(stationId));


        List<TransactionInfoDomain> totalTransactionInfos = new ArrayList<>();

        for(ChargerDomain charger : chargerList){
            List<TransactionInfoDomain> tmp = txInfoRepository.findByChargerPkAndCreatedAtBetween(domain,
                    charger.getId());
            totalTransactionInfos.addAll(tmp);
        }

        Integer allMeterValue = totalTransactionInfos.stream()
                .mapToInt(TransactionInfoDomain :: getTotalMeterValue)
                .sum();
        Integer allPrice = totalTransactionInfos.stream()
                .mapToInt(TransactionInfoDomain :: getTotalPrice)
                .sum();

        TransactionSummaryDto transactionSummaryDto = new TransactionSummaryDto(
                allMeterValue, allPrice
        );
        return transactionSummaryDto;
    }



    public Page<TransactionInfoDto> getStationTransactionsByStationAndPeriod(
            LocalDateTime start, LocalDateTime end, String stationName, Pageable pageable
    ){
        TransactionInfoDomain onlyTimeTxDomain = TransactionInfoDomain.fromLocalDateTimeToDomain(
                start
                ,end
        );
        String stationId = chargingStationRepository.findStationIdByStationName(stationName);

        Page<TransactionInfoDomain> txInfoDomains = txInfoRepository.findByStationIdAndCreatedAtBetween(
                onlyTimeTxDomain, stationId, pageable
        );
        Page<TransactionInfoDto> dtoPage = txInfoDomains.map(domain -> new TransactionInfoDto(domain));
        return dtoPage;
    }

    public TransactionInfoDto getOneTxInfo(String transactionId){
        TransactionInfoDomain txDomain = TransactionInfoDomain.transactionIdDomain(transactionId);
        TransactionInfoDomain fullTxDomain = jpaTxInfoRepository.getOneTxInfoByTxId(txDomain);
        return new TransactionInfoDto(fullTxDomain);
    }

    @Transactional
    public void saveTxInfo(CreateTransactionInfoRequest request){
        TransactionInfoDomain txInfoDomain = request.toDomain();
        jpaTxInfoRepository.save(txInfoDomain);
    }
}
