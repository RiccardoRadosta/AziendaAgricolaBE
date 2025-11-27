# Progetto Spring Boot con JWT, Firebase e Stripe

Questo progetto è un'applicazione Java basata su Spring Boot che fornisce un backend completo per la gestione di ordini, newsletter e un pannello di amministrazione. L'applicazione integra l'autenticazione basata su token JWT, la connettività a Firebase e l'elaborazione dei pagamenti con Stripe.

## Tecnologie Utilizzate

- **Spring Boot 3.2.1**: Framework per la creazione di applicazioni Java stand-alone.
- **Spring Security**: Per la gestione dell'autenticazione e autorizzazione.
- **JWT (JSON Web Token)**: Per la creazione di token di accesso per un'autenticazione sicura.
- **Firebase Admin SDK**: Per l'integrazione con i servizi di Firebase.
- **Stripe-Java**: Libreria ufficiale di Stripe per l'elaborazione dei pagamenti in Java.
- **Maven**: Per la gestione delle dipendenze e il build del progetto.
- **Java 17**: Versione del linguaggio di programmazione.

## Funzionalità Principali

### 1. Autenticazione e Sicurezza

- **Autenticazione JWT**: L'accesso alle API è protetto tramite token JWT. Un utente deve prima autenticarsi per ricevere un token, che deve poi essere incluso nelle richieste successive.
- **Filtro di Sicurezza**: Un `JwtRequestFilter` intercetta ogni richiesta per validare il token JWT e impostare l'autenticazione nel contesto di Spring Security.
- **Configurazione di Sicurezza**: La `SecurityConfig` definisce le regole di accesso, specificando quali endpoint sono pubblici e quali richiedono l'autenticazione.

### 2. Gestione degli Ordini

- **Creazione e Gestione Ordini**: Espone API per creare nuovi ordini, recuperarli e gestirli.
- **Integrazione con Stripe**: Utilizza Stripe per elaborare i pagamenti associati agli ordini.
- **DTO (Data Transfer Object)**: Utilizza `OrderDTO` per trasferire i dati degli ordini tra il client e il server in modo sicuro e pulito.

### 3. Iscrizione alla Newsletter

- **API per l'Iscrizione**: Fornisce endpoint per consentire agli utenti di iscriversi e annullare l'iscrizione a una newsletter.
- **Validazione**: Applica la validazione sui dati forniti per l'iscrizione tramite `spring-boot-starter-validation`.

### 4. Pannello di Amministrazione

- **Dashboard**: Un'area riservata agli amministratori per monitorare lo stato dell'applicazione.
- **Statistiche**: Fornisce dati aggregati, come le statistiche del pannello di controllo (`DashboardStatsDTO`) e i prodotti più venduti (`TopProductDTO`).

## Struttura del Progetto

Il progetto è organizzato nei seguenti package:

- `config`: Contiene le classi di configurazione per Firebase (`FirebaseConfig`), Stripe (`StripeConfig`) e le impostazioni web (`WebConfig`).
- `security`: Include le classi per la gestione della sicurezza, come `JwtUtil` (per la creazione e validazione dei token), `JwtRequestFilter` e `SecurityConfig`.
- `admin`: Raccoglie i controller, i servizi e i DTO relativi al pannello di amministrazione.
- `newsletter`: Gestisce la logica per le iscrizioni alla newsletter.
- `order`: Contiene le classi per la gestione degli ordini.

## Chiamate alle API

L'applicazione espone diverse API REST per interagire con il sistema. Di seguito una descrizione generale degli endpoint disponibili:

### Autenticazione

- `POST /authenticate`: Endpoint per l'autenticazione di un utente e la generazione di un token JWT. (Nota: l'implementazione specifica di questo endpoint non è visibile, ma è una parte standard della configurazione di sicurezza JWT).

### Amministrazione (`/admin`)

- `GET /dashboard`: Recupera le statistiche e i dati per il pannello di amministrazione.

### Newsletter (`/newsletter`)

- `POST /subscribe`: Permette a un utente di iscriversi alla newsletter.
- `DELETE /unsubscribe`: Consente a un utente di annullare l'iscrizione.

### Ordini (`/orders`)

- `POST /`: Crea un nuovo ordine.
- `GET /{id}`: Recupera i dettagli di un ordine specifico.
- `GET /`: Elenca tutti gli ordini (potrebbe essere un endpoint protetto per amministratori).

## Interazioni con il Database (Firestore/Firebase)

Sebbene il codice non mostri l'implementazione esatta delle interazioni con il database, l'inclusione di **Firebase Admin SDK** suggerisce che i dati vengano salvati su **Cloud Firestore** o un altro servizio di database di Firebase. Le operazioni tipiche che l'applicazione eseguirà sul database includono:

- **Salvataggio Utenti**: Memorizzazione delle informazioni degli utenti per l'autenticazione.
- **Salvataggio Iscrizioni**: Salvataggio degli indirizzi email degli iscritti alla newsletter.
- **Salvataggio Ordini**: Memorizzazione dei dettagli degli ordini, inclusi i prodotti, le quantità e lo stato del pagamento.
- **Recupero Dati**: Interrogazioni per recuperare statistiche per il pannello di amministrazione, come il numero totale di ordini o i prodotti più popolari.

## Configurazione

### Gestione delle Variabili: Sviluppo vs. Produzione

L'applicazione segue le best practice di Spring Boot per la gestione della configurazione:

-   **Per lo Sviluppo Locale**: Le configurazioni necessarie per avviare il progetto (come le chiavi per i servizi esterni) sono definite nel file `src/main/resources/application.properties`. Questo permette di essere operativi rapidamente in ambiente di sviluppo.

-   **Per la Produzione**: Per motivi di sicurezza e come pratica standard, tutte le configurazioni definite nel file `application.properties` devono essere fornite come **variabili d'ambiente** sul server di produzione. Spring Boot darà automaticamente priorità alle variabili d'ambiente, sovrascrivendo i valori di default presenti nel file. Questo assicura che nessuna chiave segreta sia mai salvata nel codice sorgente. Il file `.env.example` nel progetto elenca tutte le variabili che devono essere configurate nell'ambiente di produzione.

### Firebase

1.  Crea un progetto su [Firebase Console](https://console.firebase.google.com/).
2.  Genera una chiave dell'account di servizio (un file JSON).
3.  Imposta il percorso del file JSON nella configurazione dell'applicazione (ad esempio, tramite una variabile d'ambiente o in `application.properties`).

La `FirebaseConfig.java` si occupa di inizializzare l'app Firebase all'avvio.

### Stripe

1.  Ottieni la tua chiave segreta dall'[dashboard di Stripe](https://dashboard.stripe.com/).
2.  Configura la chiave in `StripeConfig.java` o, preferibilmente, tramite variabili d'ambiente per non esporla nel codice.

```java
// Esempio in StripeConfig.java
@Value("${stripe.api.key}")
private String stripeApiKey;

@PostConstruct
public void init() {
    Stripe.apiKey = stripeApiKey;
}
```

## Come Eseguire il Progetto

1.  **Clona il repository.**
2.  **Configura le credenziali**: Assicurati di aver impostato correttamente le chiavi per Firebase e Stripe come descritto sopra.
3.  **Compila ed esegui**: Utilizza Maven per avviare l'applicazione.
    ```bash
    ./mvnw spring-boot:run
    ```
L'applicazione sarà disponibile all'indirizzo `http://localhost:8080`.