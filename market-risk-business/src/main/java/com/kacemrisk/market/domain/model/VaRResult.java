package com.kacemrisk.market.domain.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VaRResult implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    double var;
    double expectedShortfall;
    double alpha;
    int numberOfScenarios;
    double meanPnL;
    double stdDevPnL;
}
