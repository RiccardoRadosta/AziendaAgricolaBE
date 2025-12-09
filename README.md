# Progetto Spring Boot con JWT, Firebase e Stripe

Questo progetto è un'applicazione Java basata su Spring Boot che fornisce un backend completo per la gestione di ordini, newsletter e un pannello di amministrazione. L'applicazione integra l'autenticazione basata su token JWT, la connettività a Firebase, l'elaborazione dei pagamenti con Stripe e il monitoraggio delle statistiche del sito tramite Vercel Analytics.

## Tecnologie Utilizzate

- **Spring Boot 3.2.1**: Framework per la creazione di applicazioni Java stand-alone.
- **Spring Security**: Per la gestione dell'autenticazione e autorizzazione.
- **JWT (JSON Web Token)**: Per la creazione di token di accesso per un'autenticazione sicura.
- **Firebase Admin SDK**: Per l'integrazione con i servizi di Firebase.
- **Stripe-Java**: Libreria ufficiale di Stripe per l'elaborazione dei pagamenti in Java.
- **Vercel Analytics API**: Per recuperare dati di traffico e visualizzazioni del sito deployato su Vercel.
- **Maven**: Per la gestione delle dipendenze e il build del progetto.
- **Java 17**: Versione del linguaggio di programmazione.

## Funzionalità Principali

### 1. Autenticazione e Sicurezza

- **Autenticazione JWT**: L'accesso alle API è protetto tramite token JWT.
- **Filtro di Sicurezza**: Un `JwtRequestFilter` intercetta ogni richiesta per validare il token JWT.

### 2. Gestione degli Ordini

- **Creazione e Gestione Ordini**: Espone API per creare nuovi ordini, recuperarli e gestirli.
- **Integrazione con Stripe**: Utilizza Stripe per elaborare i pagamenti.

### 3. Iscrizione alla Newsletter

- **API per l'Iscrizione**: Fornisce endpoint per consentire agli utenti di iscriversi e annullare l'iscrizione.

### 4. Pannello di Amministrazione

- **Dashboard**: Un'area riservata per monitorare lo stato dell'applicazione.
- **Statistiche**: Fornisce dati aggregati e i prodotti più venduti.
- **Visualizzazione Analytics**: Integra le statistiche di Vercel Analytics (visualizzazioni di pagina, visitatori) direttamente nel pannello.

## Struttura del Progetto

Il progetto è organizzato nei seguenti package:

- `config`: Classi di configurazione per Firebase, Stripe, etc.
- `security`: Gestione della sicurezza e dei token JWT.
- `admin`: Controller, servizi e DTO per il pannello di amministrazione (inclusa l'integrazione Vercel).
- `newsletter`: Logica per le iscrizioni alla newsletter.
- `order`: Gestione degli ordini.

## Chiamate alle API

(Sezioni per Autenticazione, Newsletter, Ordini omesse per brevità)

### Amministrazione (`/api/admin`)

- `GET /dashboard-stats`: Recupera le statistiche interne del negozio (es. ordini, ricavi).
- `GET /analytics`: Recupera i dati di traffico da Vercel Analytics. L'endpoint funge da proxy sicuro. Può essere personalizzato con i parametri query `from` (es. "24h", "7d") e `type` (es. "pageview", "visitor").

## Configurazione

### Gestione delle Variabili: Sviluppo vs. Produzione

L'applicazione utilizza un file `src/main/resources/application.properties` per lo sviluppo locale e si aspetta variabili d'ambiente in produzione, come descritto nel file `.env.example`.

### Firebase

1.  Crea un progetto su [Firebase Console](https://console.firebase.google.com/).
2.  Genera una chiave dell'account di servizio (file JSON).
3.  Imposta il percorso del file nella configurazione dell'applicazione.

### Stripe

1.  Ottieni la tua chiave segreta dall'[dashboard di Stripe](https://dashboard.stripe.com/).
2.  Configura la chiave come variabile d'ambiente o in `application.properties`.

### Configurazione Brevo per l'Invio di Email

Per inviare email transazionali (conferme d'ordine) e di servizio, è necessario configurare un servizio SMTP come Brevo, verificando il dominio del mittente e utilizzando una chiave API.

### Integrazione con Vercel Analytics

Per permettere al backend di recuperare le statistiche del sito (es. visualizzazioni di pagina, visitatori) dal pannello di Vercel, è stata implementata un'integrazione con l'API di Vercel. Il backend agisce come un proxy sicuro, proteggendo le credenziali di accesso.

**1. Creazione del Token di Accesso su Vercel**

Per autenticare le richieste, è necessario generare un token API dal proprio account Vercel:

1.  Accedi al tuo account su [vercel.com](https://vercel.com).
2.  Clicca sull'icona del tuo profilo in alto a destra e seleziona **"Settings"**.
3.  Nel menu di navigazione a sinistra, sotto "Personal Account Settings", clicca sulla voce **"Tokens"**.
4.  Clicca su **"Create"**, assegna un nome descrittivo al token (es. `Backend Analytics`) e crea il token.
5.  **Copia il token generato**. Vercel lo mostrerà solo una volta.

**2. Recupero del Project ID**

L'ID del progetto è necessario per specificare a Vercel per quale sito si stanno richiedendo i dati.

1.  Dalla dashboard di Vercel, seleziona il progetto di interesse.
2.  Vai alla tab **"Settings"** del progetto.
3.  Nella sezione **"General"**, troverai il campo **"Project ID"**. Copia questo valore.

**3. Configurazione delle Variabili**

I due valori recuperati devono essere configurati come segue:

-   **Per lo sviluppo locale**, aggiungi le seguenti righe al file `src/main/resources/application.properties`:

    ```properties
    # Vercel Analytics Configuration
    vercel.api.token=IL_TUO_TOKEN_API_VERCEL
    vercel.project.id=IL_TUO_PROJECT_ID_VERCEL
    ```

-   **Per la produzione (deploy)**, queste stesse chiavi devono essere configurate come variabili d'ambiente. Se utilizzi un file `env.yaml` per il deploy (es. su Google Cloud Run), aggiungerai:

    ```yaml
    # env.yaml
    VERCEL_API_TOKEN: 'IL_TUO_TOKEN_API_VERCEL'
    VERCEL_PROJECT_ID: 'IL_TUO_PROJECT_ID_VERCEL'
    ```

## Come Eseguire il Progetto

1.  **Clona il repository.**
2.  **Configura le credenziali**: Assicurati di aver impostato correttamente tutte le chiavi (Firebase, Stripe, Brevo, Vercel) come descritto sopra.
3.  **Compila ed esegui**: Utilizza Maven per avviare l'applicazione.
    ```bash
    ./mvnw spring-boot:run
    ```
L'applicazione sarà disponibile all'indirizzo `http://localhost:8080`.

## Implementazioni Future

### Report Giornaliero degli Ordini con Cloud Scheduler

Per inviare un report giornaliero in modo affidabile, si consiglia di utilizzare **Google Cloud Scheduler** per chiamare un endpoint API sicuro (`POST /admin/reports/send-daily-summary`), specialmente in un ambiente serverless come Cloud Run.
