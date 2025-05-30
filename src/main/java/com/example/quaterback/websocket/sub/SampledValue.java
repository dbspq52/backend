package com.example.quaterback.websocket.sub;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class SampledValue {
    private final Integer value;
    private final String measurand;

    public static SampledValue forMeterValues(Integer value, String measurand) {
        return new SampledValue(value, measurand);
    }

    public static SampledValue forTransactionEvent(Integer value) {
        return new SampledValue(value, null);
    }
}