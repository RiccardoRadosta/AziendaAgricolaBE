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
5.  [Fase 4: Deploy del Frontend su Vercel/Netlify](#fase-4-deploy-del-frontend-su-vercelnetlify)

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

Il backend è un'applicazione Spring Boot che gestisce tutta la logica di business, le API e l'integrazione con servizi esterni.

### 1. Configurazione delle Credenziali

**MAI ESEGUIRE IL COMMIT DI CHIAVI PRIVATE O FILE DI CREDENZIALI NEL REPOSITORY GIT.**

Le chiavi API e altre configurazioni sensibili vanno gestite tramite variabili d'ambiente.

1.  **Crea un file `.env.example`** nella root del progetto per elencare tutte le variabili necessarie.
2.  **Per lo sviluppo locale**: Crea un file `.env` (ignorato da Git) e inserisci i valori reali.
3.  **Per la produzione**: Le variabili verranno impostate direttamente nell'ambiente di Google Cloud Run durante il deploy.

### 2. Creazione del Dockerfile

Un `Dockerfile` efficace è cruciale. Assicurati che il comando di avvio (`ENTRYPOINT` o `CMD`) permetta a Spring Boot di accettare la porta dall'ambiente esterno, come richiesto da Cloud Run.

**Esempio di `ENTRYPOINT` per Cloud Run:**
```dockerfile
ENTRYPOINT ["java", "-jar", "/usr/local/lib/app.jar", "--server.port=${PORT:8080}"]
```
Questo comando avvia l'applicazione sulla porta fornita dalla variabile d'ambiente `PORT` (impostata da Cloud Run) e usa `8080` come fallback.

---

## Fase 2: Preparazione del Frontend

(Questa sezione presuppone che il frontend sia già configurato per leggere le sue variabili d'ambiente)

---

## Fase 3: Deploy del Backend su Google Cloud Run

Per questa fase, devi avere installato e configurato `gcloud CLI`.

1.  **Abilita le API necessarie** (da eseguire solo una volta per progetto):
    ```bash
    gcloud services enable run.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com
    ```

2.  **Crea un Repository su Artifact Registry** (da eseguire solo una volta per progetto):
    ```bash
    # Sostituisci REGION (es. europe-west1) e REPO_NAME
    gcloud artifacts repositories create REPO_NAME --repository-format=docker --location=REGION
    ```

3.  **Costruisci l'immagine Docker e caricala su Artifact Registry**:
    ```bash
    # Sostituisci REGION, PROJECT_ID, REPO_NAME, e APP_NAME
    gcloud builds submit --tag REGION-docker.pkg.dev/PROJECT_ID/REPO_NAME/APP_NAME
    ```

4. **Troubleshooting e Deploy Iterativo con Variabili d'Ambiente**

È molto comune che il primo deploy (eseguito senza variabili d'ambiente) fallisca con un errore `Container failed to start`. Questo è normale e indica che l'applicazione si è arrestata perché le manca una configurazione.

**La procedura corretta è iterativa:**

1.  **Analizza i Log**: Vai sulla console di Google Cloud, nella sezione "Log" del tuo servizio Cloud Run. Cerca un'eccezione Java che indichi la causa dell'arresto (es. `Could not resolve placeholder 'SOME_VARIABLE'`).

2.  **Identifica e Aggiungi le Variabili Mancanti**: Riesegui il deploy usando il flag `--set-env-vars` per fornire le configurazioni necessarie. Inizia con la variabile indicata dall'errore e aggiungi le altre man mano che nuovi errori appaiono.

3.  **Comando di Deploy Completo (Esempio)**: Una volta identificate tutte le variabili, il tuo comando di deploy sarà simile a questo. Le variabili vanno passate come una singola stringa separata da virgole. **Sostituisci tutti i valori segnaposto (`YOUR_...`) con le tue chiavi reali.**

    ```bash
    # Sostituisci i segnaposto generici (APP_NAME, REGION, etc.) e quelli delle variabili (YOUR_...)
    gcloud run deploy APP_NAME \
      --image REGION-docker.pkg.dev/PROJECT_ID/REPO_NAME/APP_NAME \
      --platform managed \
      --region REGION \
      --allow-unauthenticated \
      --set-env-vars="GOOGLE_CLOUD_PROJECT=YOUR_PROJECT_ID,STRIPE_SECRET_KEY=YOUR_STRIPE_SECRET_KEY,BREVO_API_KEY=YOUR_BREVO_API_KEY,CLOUDINARY_CLOUD_NAME=YOUR_CLOUDINARY_CLOUD_NAME,CLOUDINARY_API_KEY=YOUR_CLOUDINARY_API_KEY,CLOUDINARY_API_SECRET=YOUR_CLOUDINARY_API_SECRET,ADMIN_USERNAME=YOUR_ADMIN_USERNAME,ADMIN_PASSWORD=YOUR_ADMIN_PASSWORD,CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com"
    ```

Questo processo va ripetuto finché tutte le dipendenze di configurazione non sono soddisfatte e il servizio si avvia correttamente.

---

## Fase 4: Deploy del Frontend su Vercel/Netlify

1.  **Importa il Progetto**: Collega il tuo repository Git (la cartella del frontend) alla piattaforma scelta (Vercel, Netlify, etc.).
2.  **Configura il Build**: Tipicamente `npm run build` come comando e `dist` come cartella di pubblicazione.
3.  **Aggiungi le Variabili d'Ambiente**: Nel pannello di controllo della piattaforma, inserisci le variabili per il frontend (es. `VITE_API_BASE_URL` con l'URL del backend di Cloud Run, la chiave pubblica di Stripe, etc.).
4.  **Esegui il Deploy** e aggiorna le impostazioni CORS del backend con il nuovo URL del frontend, se necessario.
