# Imagem da API para publicação (Render). Duas etapas: compila com o JDK e roda só com o JRE.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Dependências numa camada própria: só baixa de novo quando o pom.xml muda.
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Plano gratuito do Render: 512 MB de memória para a JVM inteira.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
# O Render informa a porta na variável PORT.
CMD ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar app.jar"]
