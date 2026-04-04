package infrastructure.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class EnrichedPositionRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String portfolioId;
    private String ticker;
    private double quantity;
    private String assetClass;
    private double spotPrice;
}

