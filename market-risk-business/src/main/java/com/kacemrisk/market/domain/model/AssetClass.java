package com.kacemrisk.market.domain.model;

public enum AssetClass {

    EQD, CTY, FX, IRD;

    public static AssetClass of(String assetClass) {
        return switch (assetClass) {
            case "EQD" -> AssetClass.EQD;
            case "CTY" -> AssetClass.CTY;
            case "FX" -> AssetClass.FX;
            case "IRD" -> AssetClass.IRD;
            default -> null;
        };
    }
}
