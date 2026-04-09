package com.kacemrisk.market.infrastructure.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class EnrichedPositionRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String portfolioId;
    private String ticker;
    private double quantity;
    private String assetClass;
    private double spotPrice;
}

