package infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RiskPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskPlatformApplication.class, args);
    }
}
