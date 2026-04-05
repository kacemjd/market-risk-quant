package domain.bdd.steps;

import domain.model.MarketData;
import domain.model.Portoflio;
import domain.model.Position;
import domain.service.simulation.analytical.ParametricVaRCalculator;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParametricVaRSteps {

    private static final String TICKER = "CRTO";
    // Simplified regulatory convention: 100 trading days per year (sqrt(100) = 10)
    private static final double TRADING_DAYS_SQRT = 10.0;

    private final ParametricVaRCalculator calculator = new ParametricVaRCalculator();

    private int quantity;
    private double spotPrice;
    private double annualVol;
    private double varResult;

    @Given("a portfolio with a single equity position of {int} shares at spot price {double}")
    public void aPortfolioWithASingleEquityPosition(int quantity, double spotPrice) {
        this.quantity = quantity;
        this.spotPrice = spotPrice;
    }

    @And("a volatility of {int}% for that asset")
    public void aVolatilityOfPercentForThatAsset(int volatilityPct) {
        this.annualVol = volatilityPct / 100.0;
    }

    @When("I calculate the VaR at {int}% confidence")
    public void iCalculateTheVaRAtConfidence(int confidencePct) {
        double alpha = confidencePct / 100.0;

        Position position = Position.equitySpot(TICKER, quantity, spotPrice);

        Portoflio portfolio = Portoflio.builder()
                .id("test-portfolio")
                .positions(List.of(position))
                .build();

        double dailyVol = annualVol / TRADING_DAYS_SQRT;
        double[][] covMatrix = {{dailyVol * dailyVol}};

        MarketData marketData = MarketData.builder()
                .asOfDate(LocalDate.now())
                .volatilities(Map.of(TICKER, annualVol))
                .correlationMatrix(new double[][]{{1.0}})
                .covarianceMatrix(covMatrix)
                .riskFactors(List.of(TICKER))
                .build();

        varResult = calculator.calculate(portfolio, marketData, alpha).getVar();
    }

    @Then("the VaR should be positive")
    public void theVaRShouldBePositive() {
        assertTrue(varResult > 0, "VaR must be positive, got: " + varResult);
    }

    @And("the VaR should be approximately {double} with a tolerance of {double}")
    public void theVaRShouldBeApproximately(double expected, double tolerance) {
        assertEquals(expected, varResult, tolerance,
                "VaR expected ~" + expected + " ± " + tolerance + " but got " + varResult);
    }
}

