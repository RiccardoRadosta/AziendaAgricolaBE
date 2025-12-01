<div align="center">
  <img src="https://storage.googleapis.com/datakaizen-website-bucket/logo_datakaizen.png" alt="Logo" width="150"/>
  <h1>Guida al Deploy - Applicazione "Azienda Agricola"</h1>
</div>

---

Questo documento descrive i passaggi necessari per eseguire il deploy dell'applicazione full-stack "Azienda Agricola", composta da un backend in Java Spring Boot e un frontend in Vite (React/Vue).

## Indice

1.  [Prerequisiti](#prerequisiti)
2.  [Fase 1: Preparazione del Backend](#fase-1-preparazione-del-backend)
3.  [Fase 2: Preparazione del Frontend](#fase-2-preparazione-del-frontend)
4.  [Fase 3: Deploy del Backend su Google Cloud Run](#fase-3-deploy-del-backend-su-google-cloud-run)
5.  [Fase 4: Deploy del Frontend](#fase-4-deploy-del-frontend)

---

## Prerequisiti

Prima di iniziare, assicurati di avere installato e configurato i seguenti strumenti:

*   **Git**: Per la gestione del codice sorgente.
*   **Java 17+**: Per eseguire il backend in locale.
*   **Maven**: Per la gestione delle dipendenze del backend.
*   **Node.js e npm/yarn**: Per la gestione del frontend.
*   **Google Cloud SDK (`gcloud CLI`)**: Per interagire con Google Cloud.
*   **Docker**: Per containerizzare il backend.

---

## Fase 1: Preparazione del Backend

Il backend è un'applicazione Spring Boot che gestisce tutta la logica di business e le API. La configurazione delle credenziali è gestita tramite Profili Spring per separare l'ambiente di sviluppo da quello di produzione.

### 1. Configurazione tramite Profili Spring

Il progetto utilizza due file di properties principali:
- `src/main/resources/application.properties`: Usato per lo sviluppo locale (`dev` profile).
- `src/main/resources/application-prod.properties`: Usato per il deploy in produzione (`prod` profile).

Il profilo attivo viene scelto in base alla variabile d'ambiente `SPRING_PROFILES_ACTIVE`.

### 2. Gestione delle Credenziali Firebase/Google Cloud

La connessione a Firebase/Firestore viene gestita in modo diverso a seconda dell'ambiente:

-   **Sviluppo Locale (`dev` profile):**
    -   In `application.properties`, il percorso del file delle credenziali è specificato tramite una variabile d'ambiente, ad es:
        ```properties
        firebase.credentials.path=${FIREBASE_CREDENTIALS_PATH}
        ```
    -   La classe `SecurityConfig.java` legge questo percorso e inizializza Firebase da un file di sistema.

-   **Produzione (`prod` profile):**
    -   In `application-prod.properties`, il percorso è impostato sul classpath:
        ```properties
        firebase.credentials.path=classpath:serviceAccountKey.json
        ```
    -   La classe `SecurityConfig.java` rileva il profilo `prod`, ignora il percorso del file e inizializza Firebase usando le **Credenziali Predefinite dell'Applicazione (Application Default Credentials)** fornite dall'ambiente Cloud Run. Questo elimina la necessità di gestire file di chiavi nel container.
    -   **È FONDAMENTALE** associare il corretto Service Account al servizio Cloud Run durante il deploy.

**MAI ESEGUIRE IL COMMIT DI CHIAVI PRIVATE O FILE DI CREDENZIALI NEL REPOSITORY GIT.**

---

## Fase 2: Preparazione del Frontend

(Questa sezione presuppone che il frontend sia già configurato per leggere le sue variabili d'ambiente)

---

## Fase 3: Deploy del Backend su Google Cloud Run

Per questa fase, devi avere installato e configurato `gcloud CLI` e aver effettuato l'accesso.

1.  **Abilita le API necessarie** (da eseguire solo una volta per progetto):
    ```bash
    gcloud services enable run.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com
    ```

2.  **Crea un Repository su Artifact Registry** (da eseguire solo una volta per progetto):
    ```bash
    # Sostituisci REGION (es. europe-west1)
    gcloud artifacts repositories create azienda-agricola-repo --repository-format=docker --location=europe-west1
    ```

3.  **Costruisci l'immagine Docker e caricala su Artifact Registry**:
    Questo comando prende il codice, lo pacchettizza in un'immagine Docker e lo spinge nel repository di Artifact Registry.
    ```bash
    gcloud builds submit --tag europe-west1-docker.pkg.dev/base-be-azienda/azienda-agricola-repo/azienda-agricola-backend
    ```

4. **Deploy su Cloud Run con Configurazione Completa**

Il deploy finale richiede di specificare l'immagine appena creata, la regione, il service account e tutte le variabili d'ambiente necessarie all'applicazione per avviarsi.

**Il comando di deploy completo e corretto è il seguente.** Sostituisci i valori segnaposto (`YOUR_...`) con le tue chiavi reali.

```bash
gcloud run deploy azienda-agricola-backend \
  --image europe-west1-docker.pkg.dev/base-be-azienda/azienda-agricola-repo/azienda-agricola-backend \
  --platform managed \
  --region europe-west1 \
  --allow-unauthenticated \
  --service-account 272598566542-compute@developer.gserviceaccount.com \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod,GOOGLE_CLOUD_PROJECT=base-be-azienda,BREVO_API_KEY=YOUR_BREVO_API_KEY,BREVO_SENDER_EMAIL=YOUR_BREVO_SENDER_EMAIL,BREVO_API_URL=https://api.brevo.com/v3/smtp/email,CLOUDINARY_CLOUD_NAME=YOUR_CLOUDINARY_NAME,CLOUDINARY_API_KEY=YOUR_CLOUDINARY_API_KEY,CLOUDINARY_API_SECRET=YOUR_CLOUDINARY_SECRET,CORS_ALLOWED_ORIGINS=*,STRIPE_SECRET_KEY=YOUR_STRIPE_SECRET_KEY"
```

**Note importanti sul comando:**
-   `--service-account`: Associa l'identità corretta al servizio, fondamentale per l'autenticazione con le API di Google Cloud (es. Firestore).
-   `--set-env-vars`: Fornisce tutte le chiavi API e le configurazioni. **`SPRING_PROFILES_ACTIVE=prod` è cruciale** per attivare la configurazione di produzione.
-   `--allow-unauthenticated`: Permette al servizio di essere raggiunto pubblicamente. La sicurezza delle rotte è gestita internamente da Spring Security.

---

## Fase 4: Deploy del Frontend

1.  **Importa il Progetto**: Collega il tuo repository Git (la cartella del frontend) a una piattaforma come Vercel o Netlify.
2.  **Configura il Build**: Tipicamente `npm run build` come comando e `dist` come cartella di pubblicazione.
3.  **Aggiungi le Variabili d'Ambiente**: Nel pannello di controllo della piattaforma, inserisci le variabili per il frontend (es. `VITE_API_BASE_URL` con l'URL del backend di Cloud Run, la chiave pubblica di Stripe, etc.).
4.  **Esegui il Deploy** e assicurati che il valore di `CORS_ALLOWED_ORIGINS` nel backend includa il dominio del frontend deployato.
