package workflow;

import domain.model.MaturityGrid;
import domain.model.VaRMethod;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class ScenarioNotification {

    String     correlationId;
    String     portfolioCsvPath;
    String     pricesCsvPath;
    LocalDate  asOfDate;

    @Builder.Default VaRMethod   varMethod       = VaRMethod.MONTE_CARLO;
    @Builder.Default double      confidenceLevel = 0.99;
    @Builder.Default int         numPaths        = 10_000;
    @Builder.Default int         historicalWindow = 500;        // 250 = Basel II min, 500 = industry standard
    @Builder.Default MaturityGrid timeGrid       = MaturityGrid.GRID_53;
}

