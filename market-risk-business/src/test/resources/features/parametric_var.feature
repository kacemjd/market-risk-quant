Feature: Parametric VaR calculation
  As a risk manager
  I want to compute the parametric VaR of a portfolio
  So that I can report regulatory capital requirements

  Scenario: Single-asset equity portfolio at 95% confidence
    Given a portfolio with a single equity position of 1000 shares at spot price 100.0
    And a volatility of 20% for that asset
    When I calculate the VaR at 95% confidence
    Then the VaR should be positive
    And the VaR should be approximately 3290.0 with a tolerance of 10.0

  Scenario: Single-asset equity portfolio at 99% confidence
    Given a portfolio with a single equity position of 1000 shares at spot price 100.0
    And a volatility of 20% for that asset
    When I calculate the VaR at 99% confidence
    Then the VaR should be positive
    And the VaR should be approximately 4652.0 with a tolerance of 10.0

