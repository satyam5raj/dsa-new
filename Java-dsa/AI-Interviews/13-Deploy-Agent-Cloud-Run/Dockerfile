# Multi-stage build: compile with the JDK, run on a small JRE image.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY AgentServer.java .
RUN javac -d /out AgentServer.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /out /app
# Cloud Run sets $PORT (default 8080); the app reads it.
ENV PORT=8080
# Run as a non-root user.
RUN useradd --system --uid 10001 agent
USER agent
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-cp", "/app", "AgentServer", "serve"]
