package com.example.quaterback.api.domain.charger.entity;

import com.example.quaterback.api.domain.charger.constant.ChargerStatus;
import com.example.quaterback.api.domain.charger.domain.ChargerDomain;
import com.example.quaterback.api.domain.station.entity.ChargingStationEntity;
import com.example.quaterback.api.domain.txinfo.entity.TransactionInfoEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "charger_info")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer evseId;

    @Enumerated(EnumType.STRING)
    private ChargerStatus chargerStatus;

    private LocalDateTime updateStatusTimeStamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", referencedColumnName = "stationId")
    private ChargingStationEntity station;
    //@OneToMany(mappedBy ="evseId", cascade = CascadeType.ALL)
    //private List<TransactionInfoEntity> txInfoList = new ArrayList<>();


/***연관 관계 메서드 작성 필요!!!!***/


    public ChargerDomain toDomain() {
        return ChargerDomain.builder()
                .stationId(station.getStationId())
                .evseId(evseId)
                .chargerStatus(chargerStatus)
                .updateStatusTimeStamp(updateStatusTimeStamp)
                .build();
    }

    public void updateChargerStatus(ChargerStatus status) {
        if (!status.equals(chargerStatus)) {
            chargerStatus = status;
            updateStatusTimeStamp = LocalDateTime.now();
        }
    }

    public void assignStation(ChargingStationEntity stationEntity) {
        station = stationEntity;
        if (!stationEntity.getChargerList().contains(this)) {
            stationEntity.getChargerList().add(this);
        }
    }
}
