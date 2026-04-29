package com.kacemrisk.market.domain.model;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Value
@Builder
public class Portfolio implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    String id;
    List<Position> positions;

    public double getTotalValue() {
        return positions.stream()
                .mapToDouble(Position::getNotional)
                .sum();
    }
}
