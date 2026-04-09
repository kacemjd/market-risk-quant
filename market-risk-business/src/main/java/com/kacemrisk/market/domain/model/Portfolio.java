package com.kacemrisk.market.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Portfolio {

    String id;
    List<Position> positions;

    public double getTotalValue() {
        return positions.stream()
                .mapToDouble(Position::getNotional)
                .sum();
    }
}
