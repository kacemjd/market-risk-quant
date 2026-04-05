package application.port.in;

import domain.model.MarketData;
import domain.model.MaturityGrid;
import domain.model.Portoflio;
import domain.model.VaRMethod;
import lombok.Builder;
import lombok.Value;

/**
 * Command object carrying all inputs required by the Application layer
 * to execute a VaR calculation.
 */
@Value
@Builder
public class CalculateVaRCommand {
    Portoflio portfolio;
    MarketData marketData;

    VaRMethod method;
    double alpha;

    // Configuration parameters for specific strategies
    int numPaths;
    int historicalWindow;
    MaturityGrid timeGrid;
}

