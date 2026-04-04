package infrastructure.integration;

import domain.model.MarketData;
import infrastructure.RiskPlatformApplication;
import infrastructure.adapter.in.spark.SparkMarketDataIngestionAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RiskPlatformApplication.class)
class SparkMarketDataIngestionIT {

    private static final String TICKER    = "NVDA";
    private static final LocalDate AS_OF  = LocalDate.of(2017, 11, 10);

    @Autowired
    private SparkMarketDataIngestionAdapter ingestionAdapter;

    @Test
    void should_calibrate_nvda_market_data_from_csv() throws Exception {
        URL resource = getClass().getClassLoader().getResource("market-data/nvda.csv");
        assertThat(resource).isNotNull();
        String csvPath = Paths.get(resource.toURI()).toString();

        MarketData result = ingestionAdapter.ingest(TICKER, csvPath, AS_OF);

        assertThat(result).isNotNull();
        assertThat(result.getAsOfDate()).isEqualTo(AS_OF);
        assertThat(result.getRiskFactors()).containsExactly(TICKER);

        double vol = result.getVolFor(TICKER);
        assertThat(vol).isGreaterThan(0.0);
        assertThat(vol).isBetween(0.20, 2.0);

        double[][] cov = result.getCovarianceMatrix();
        assertThat(cov).hasDimensions(1, 1);
        assertThat(cov[0][0]).isEqualTo(vol * vol, org.assertj.core.api.Assertions.within(1e-9));

        double[][] corr = result.getCorrelationMatrix();
        assertThat(corr[0][0]).isEqualTo(1.0, org.assertj.core.api.Assertions.within(1e-9));
    }
}

