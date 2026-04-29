# ── Stage 1 – Build ─────────────────────────────────────────────────────────
# Builds the multi-module Maven project and produces a fat JAR for the
# market-risk-processing module.
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy the full project (parent POM + all modules) so inter-module deps resolve
COPY pom.xml                                   ./pom.xml
COPY market-risk-business/pom.xml              market-risk-business/pom.xml
COPY market-risk-business/src                  market-risk-business/src
COPY market-risk-workflow/pom.xml              market-risk-workflow/pom.xml
COPY market-risk-workflow/src                  market-risk-workflow/src
COPY market-risk-processing/pom.xml            market-risk-processing/pom.xml
COPY market-risk-processing/src                market-risk-processing/src

# Build only the processing module and its dependencies; skip tests for speed
RUN mvn --no-transfer-progress -f pom.xml clean package -DskipTests \
        -pl market-risk-processing -am

# ── Stage 2 – Runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the Spring Boot fat JAR from the build stage
COPY --from=builder \
     /workspace/market-risk-processing/target/market-risk-processing-*.jar \
     app.jar

# Expose the servlet port (activated when the 'docker' Spring profile is active)
EXPOSE 8080

# JVM flags:
#   -Xmx2g          — Spark local[1] needs headroom alongside the Spring context
#   -XX:+UseG1GC    — suits long-lived JVM processes
ENTRYPOINT ["java", \
  "-Xmx2g", \
  "-XX:+UseG1GC", \
  "-Dspring.profiles.active=docker,questdb", \
  "-jar", "app.jar"]

