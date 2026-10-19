FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
USER appuser
COPY --from=build /app/target/library-service-*.jar app.jar
EXPOSE 8098
ENTRYPOINT ["java", "-jar", "app.jar"]
