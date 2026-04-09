package com.kacemrisk.market.infrastructure.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class VaRResultRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String correlationId;
    private String portfolioId;
    private String asOfDate;
    private double varAmount;
    private double alpha;
    private int    numberOfScenarios;
    private double meanPnL;
    private double stdDevPnL;
}

