# Imagem da API para publicacao (ex.: Render). Contexto de build = RAIZ do repositorio,
# porque as migrations ficam em database/migrations (fonte unica, empacotada pelo pom.xml):
#   docker build -f backend/Dockerfile -t bee-good-food-api .

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY database/migrations database/migrations
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/pom.xml backend/
WORKDIR /src/backend
# Checkout no Windows pode trazer CRLF no wrapper.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY backend/src src
RUN ./mvnw -B -q -DskipTests package && cp target/bee-good-food-api-*.jar /src/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 bee
COPY --from=build /src/app.jar app.jar
USER bee
# Variaveis obrigatorias em tempo de execucao: DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET_BASE64.
# PORT vem da plataforma (Render); fora dela, 8080.
ENV SERVER_ADDRESS=0.0.0.0 JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
CMD ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8080}"]
