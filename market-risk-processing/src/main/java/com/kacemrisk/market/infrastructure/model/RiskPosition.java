package com.kacemrisk.market.infrastructure.model;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.PortfolioFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskPosition implements Serializable, PortfolioFactory.RiskPositionView {

    @Serial
    private static final long serialVersionUID = 1L;

    private String portfolioId;
    private String ticker;
    private double quantity;
    private AssetClass assetClass;
    private double spotPrice;
    private double[] priceHistory;
}

