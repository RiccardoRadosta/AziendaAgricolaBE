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

### Configurazione Brevo per l'Invio di Email

Per l'invio delle email transazionali (es. conferma d'ordine) e di servizio (es. report giornalieri), il progetto utilizza Brevo (o un servizio SMTP equivalente). È fondamentale configurare correttamente il mittente per garantire la consegna delle email e mantenere una buona reputazione.

**Mittente Verificato**

Brevo richiede che qualsiasi indirizzo email utilizzato come mittente sia prima verificato per dimostrare che hai il permesso di spedire per suo conto.

**Due Tipi di Mittenti:**

1.  **Indirizzo Email Personale (es. `tuamail@gmail.com`)**
    *   **Verifica**: Per verificare un indirizzo Gmail, Brevo invia un'email di conferma direttamente a quella casella. Dovrai cliccare su un link per completare il processo.
    *   **Uso**: Adatto per test e sviluppo, ma meno professionale per la produzione. Non puoi creare indirizzi "alias" come `noreply.tuamail@gmail.com`.

2.  **Indirizzo Email su Dominio di Proprietà (es. `noreply@tuodominio.com`)**
    *   **Prerequisito**: Devi essere il proprietario del dominio (es. `tuodominio.com`).
    *   **Verifica**: Per verificare un intero dominio, Brevo ti chiederà di aggiungere dei record DNS specifici (come SPF, DKIM) nel pannello di configurazione del tuo provider di dominio. Questo dimostra che sei autorizzato a spedire da *qualsiasi* indirizzo sotto quel dominio (`contatti@`, `support@`, `noreply@`, etc.).
    *   **Uso**: È la soluzione **raccomandata e più professionale** per la produzione. L'indirizzo `noreply@tuodominio.com` non è una vera casella di posta, ma un'etichetta che indica al destinatario che l'email è automatica e non deve rispondere.

**Variabili d'Ambiente Consigliate:**

Per gestire facilmente i mittenti, si consiglia di utilizzare le seguenti variabili d'ambiente:

*   `SENDER_EMAIL`: Per le comunicazioni dirette con i clienti (es. `info@tuodominio.com`).
*   `NOREPLY_SENDER_EMAIL`: Per le email automatiche e i report (es. `noreply@tuodominio.com`).
*   `BREVO_API_KEY`: Per autenticare le richieste verso l'API di Brevo.

## Come Eseguire il Progetto

1.  **Clona il repository.**
2.  **Configura le credenziali**: Assicurati di aver impostato correttamente le chiavi per Firebase e Stripe come descritto sopra.
3.  **Compila ed esegui**: Utilizza Maven per avviare l'applicazione.
    ```bash
    ./mvnw spring-boot:run
    ```
L'applicazione sarà disponibile all'indirizzo `http://localhost:8080`.

## Implementazioni Future

### Report Giornaliero degli Ordini con Cloud Scheduler

Per inviare un report giornaliero degli ordini in modo affidabile e a costo zero, l'approccio consigliato è quello di utilizzare **Google Cloud Scheduler** in combinazione con un endpoint API dedicato, specialmente se l'applicazione è deployata su una piattaforma serverless come Google Cloud Run.

**Problema dell'Approccio Tradizionale (`@Scheduled`)**

L'annotazione `@Scheduled` di Spring Boot non è adatta per un ambiente serverless che scala le istanze a zero (come Cloud Run nel suo funzionamento di default). Se l'applicazione non riceve traffico, viene spenta, e il timer interno di Spring non può attivarsi, causando la mancata esecuzione del task.

**Soluzione Proposta (Serverless-Friendly)**

1.  **Creare un Endpoint API Sicuro**:
    *   Implementare un nuovo endpoint privato nel `AdminController`, ad esempio `POST /admin/reports/send-daily-summary`.
    *   La logica per recuperare gli ordini delle ultime 24 ore e inviare l'email di riepilogo andrà inserita all'interno di questo endpoint.
    *   L'endpoint deve essere protetto per assicurarsi che solo chiamate autorizzate possano attivarlo.

2.  **Configurare Google Cloud Scheduler**:
    *   Dalla console di Google Cloud, creare un nuovo **Cloud Scheduler Job**.
    *   **Frequenza**: Impostare la frequenza desiderata usando la sintassi cron (es. `0 6 * * *` per ogni giorno alle 6:00 del mattino).
    *   **Target**: Configurare il job per inviare una richiesta HTTP (`POST`) all'URL completo dell'endpoint creato al punto 1 (es. `https://<il-tuo-servizio>.a.run.app/admin/reports/send-daily-summary`).
    *   **Autenticazione**: Configurare il job per includere un token di autenticazione (es. un token di servizio OIDC) nella richiesta, in modo da poter chiamare in sicurezza l'endpoint protetto.

**Vantaggi di Questo Approccio**

*   **Affidabilità**: L'esecuzione è garantita da un servizio Google progettato per questo scopo.
*   **Efficienza dei Costi**: Sfrutta il piano gratuito di Cloud Scheduler (fino a 3 job gratuiti al mese) e permette a Cloud Run di scalare a zero, minimizzando i costi.
*   **Best Practice**: È l'architettura standard e raccomandata per eseguire task pianificati in un ambiente serverless.
