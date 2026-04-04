package workflow;

import domain.model.MaturityGrid;
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

    @Builder.Default double      confidenceLevel = 0.99;
    @Builder.Default int         numPaths        = 10_000;
    @Builder.Default MaturityGrid timeGrid       = MaturityGrid.GRID_53;
}

