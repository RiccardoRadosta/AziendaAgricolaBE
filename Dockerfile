# Fase 1: Build - Usa un'immagine con Maven e JDK per compilare il progetto
FROM maven:3.8.5-openjdk-17 AS build

# Copia il codice sorgente
COPY src /home/app/src
COPY pom.xml /home/app

# Esegui il build di Maven per creare il file .jar
RUN mvn -f /home/app/pom.xml clean package

# Fase 2: Run - Usa un'immagine JRE più leggera per l'esecuzione
FROM eclipse-temurin:17-jre-alpine

# Copia solo il .jar dalla fase di build
COPY --from=build /home/app/target/demo-0.0.1-SNAPSHOT.jar /usr/local/lib/app.jar

# Esponi la porta interna del container
EXPOSE 8080

# Comando per avviare l'applicazione
ENTRYPOINT ["java","-jar","/usr/local/lib/app.jar"]