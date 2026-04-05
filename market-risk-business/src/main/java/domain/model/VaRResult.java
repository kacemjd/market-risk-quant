package domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VaRResult {

    double var;
    double expectedShortfall;
    double alpha;
    int numberOfScenarios;
    double meanPnL;
    double stdDevPnL;
}
