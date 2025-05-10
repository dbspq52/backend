package com.example.quaterback.websocket;

import com.example.quaterback.api.domain.station.constant.StationStatus;
import com.example.quaterback.api.domain.station.domain.ChargingStationDomain;
import com.example.quaterback.api.domain.station.repository.ChargingStationRepository;
import com.example.quaterback.common.redis.service.RedisHeartbeatMonitorService;
import com.example.quaterback.common.redis.service.RedisMapSessionToStationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class InactiveStationService {

    private final RedisMapSessionToStationService mapService;
    private final RedisHeartbeatMonitorService heartbeatMonitorService;
    private final ChargingStationRepository chargingStationRepository;
    private final RedisTemplate<String, String> stringRedisTemplate;

    private static final long HEARTBEAT_INTERVAL_MS = 30_000;

    @Transactional
    public void cleanInactiveStationsWithTx() {
        Set<String> keys = stringRedisTemplate.keys("sessionId:*");
        if (keys == null || keys.isEmpty()) return;

        for (String sessionKey : keys) {
            String sessionId = sessionKey.replace("sessionId:", "");
            String stationId = mapService.getStationId(sessionId);
            if (stationId == null) continue;

            Long lastHeartbeat = heartbeatMonitorService.getLastHeartbeat(stationId);
            if (lastHeartbeat == null || isExpired(lastHeartbeat)) {
                ChargingStationDomain domain = chargingStationRepository.findByStationId(stationId);
                if (domain != null) {
                    domain.updateStationStatus(StationStatus.INACTIVE);
                    chargingStationRepository.update(domain);
                    log.info("Station [{}] marked INACTIVE due to expired heartbeat", stationId);
                } else {
                    log.warn("Station [{}] not found in DB", stationId);
                }

                heartbeatMonitorService.removeHeartbeat(stationId);
                mapService.removeMapping(sessionId);
            }
        }
    }

    private boolean isExpired(Long lastTimestamp) {
        return Instant.now().toEpochMilli() - lastTimestamp > HEARTBEAT_INTERVAL_MS;
    }
}
