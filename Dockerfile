# Usa un'immagine base con Maven e JDK 17 per la fase di build
FROM maven:3.8.5-openjdk-17 AS build

# Imposta la directory di lavoro
WORKDIR /home/app

# Copia il file pom.xml e scarica le dipendenze
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia il resto del codice sorgente
COPY src ./src

# Compila l'applicazione e crea il .jar
RUN mvn package -DskipTests

# Usa un'immagine base più leggera per l'ambiente di runtime
FROM openjdk:17-jdk-slim

# Copia solo il .jar dalla fase di build
COPY --from=build /home/app/target/demo-0.0.1-SNAPSHOT.jar /usr/local/lib/app.jar

# Esponi la porta interna del container (verrà mappata da Cloud Run)
EXPOSE 8080

# Comando per avviare l'applicazione, usando la variabile PORT fornita da Cloud Run
ENTRYPOINT ["java", "-jar", "/usr/local/lib/app.jar", "--server.port=${PORT:8080}"]
