# Guida al Deploy con Docker e Google Cloud Run

Questa guida ti accompagnerà nel processo di deploy del backend usando Docker e Google Cloud Run, una piattaforma serverless moderna e scalabile.

---

## Fase 1: Gestione delle Credenziali (Backend)

Prima di iniziare, devi raccogliere tutte le credenziali necessarie. Per la produzione, queste verranno gestite in modo sicuro come **variabili d'ambiente** e **segreti** su Google Cloud.

### 1. Credenziali Firebase (`GOOGLE_APPLICATION_CREDENTIALS`)

- **Azione**: Ottieni il file `.json` delle credenziali dal tuo account di servizio su Google Cloud, come già descritto in precedenza.
- **Deploy (Google Secret Manager)**:
  1.  Vai su [Google Secret Manager](https://console.cloud.google.com/security/secret-manager).
  2.  Crea un nuovo segreto (es. `firebase-credentials`).
  3.  **Copia e incolla l'intero contenuto del tuo file `.json`** nel campo "Valore del secret".
  4.  Concedi all'account di servizio di Cloud Run l'accesso a questo segreto (Ruolo: `Accessor secret di Secret Manager`).

### 2. Altre Credenziali (Stripe, JWT, Admin, etc.)

- **Azione**: Raccogli tutte le altre credenziali (chiavi Stripe, segreto JWT, credenziali admin, etc.) come descritto nel file `.env.example`.
- **Deploy**: Queste credenziali verranno inserite direttamente come **variabili d'ambiente** durante la configurazione del servizio su Cloud Run.

### 3. Dominio del Frontend (`CORS_ALLOWED_ORIGINS`)

- **Azione**: Prendi nota dell'URL dove verrà deployato il tuo frontend (es. `https://www.il-mio-sito.com`).
- **Deploy**: Questo URL sarà il valore della variabile d'ambiente `CORS_ALLOWED_ORIGINS` del tuo servizio Cloud Run.

---

## Fase 2: Creazione del `Dockerfile`

Per eseguire il deploy su Cloud Run, l'applicazione deve essere "containerizzata" usando Docker. Aggiungi un file chiamato `Dockerfile` (senza estensione) nella cartella principale del progetto con il seguente contenuto:

```Dockerfile
# Fase 1: Build - Usa un'immagine con Maven e JDK per compilare il progetto
FROM maven:3.8.5-openjdk-17 AS build

# Copia il codice sorgente
COPY src /home/app/src
COPY pom.xml /home/app

# Esegui il build di Maven per creare il file .jar
RUN mvn -f /home/app/pom.xml clean package

# Fase 2: Run - Usa un'immagine JRE più leggera per l'esecuzione
FROM openjdk:17-jre-slim

# Copia solo il .jar dalla fase di build
COPY --from=build /home/app/target/demo-0.0.1-SNAPSHOT.jar /usr/local/lib/app.jar

# Esponi la porta interna del container
EXPOSE 8080

# Comando per avviare l'applicazione
ENTRYPOINT ["java","-jar","/usr/local/lib/app.jar"]
```

Questo file è riutilizzabile e va salvato nel tuo repository Git.

---

## Fase 3: Deploy del Backend su Google Cloud Run

Per questa fase, devi avere installato e configurato `gcloud CLI` sul tuo computer.

1.  **Abilita le API necessarie**:
    ```bash
    gcloud services enable run.googleapis.com artifactregistry.googleapis.com
    ```

2.  **Costruisci l'immagine Docker e caricala su Google Artifact Registry** (sostituisci `PROJECT_ID` e `APP_NAME`):
    ```bash
    gcloud builds submit --tag gcr.io/PROJECT_ID/APP_NAME
    ```

3.  **Deploya l'immagine su Cloud Run**, impostando tutte le variabili d'ambiente. Questo è un esempio con le variabili più importanti:
    ```bash
    gcloud run deploy APP_NAME \
      --image gcr.io/PROJECT_ID/APP_NAME \
      --platform managed \
      --region europe-west1 `# Scegli la tua regione` \
      --allow-unauthenticated \
      --set-env-vars="CORS_ALLOWED_ORIGINS=https://www.il-mio-sito.com" \
      --set-env-vars="STRIPE_SECRET_KEY=sk_live_..." \
      --set-env-vars="JWT_SECRET=il_tuo_segreto_molto_lungo" `# Aggiungi le altre con la stessa sintassi`
    ```
    
    *Nota: `gcloud` ti chiederà di impostare altre opzioni interattivamente al primo deploy.*

Al termine, `gcloud` ti fornirà un **URL pubblico** per il tuo backend (es. `https://app-name-xyz-ew.a.run.app`).

---

## Fase 4: Configurazione e Deploy del Frontend

Il frontend (basato su Vite) deve essere configurato per usare variabili d'ambiente diverse per lo sviluppo locale e la produzione.

### 1. Sviluppo Locale

Per lo sviluppo, crea un file `.env` nella cartella principale del progetto frontend. **Questo file non deve essere caricato su Git.**

**File `.env`:**
```
# Variabili d'ambiente per lo sviluppo LOCALE

VITE_API_BASE_URL=http://localhost:8080/api
VITE_STRIPE_PUBLIC_KEY=pk_test_51SSyIFJyUjcsusLXP52MDwLIuhdHikABiK0qyU0VUaP0fCQ27UyZ9G1m6b7hV8Qu73JE22KxH932oQnbrMq3vyCH00faWDvzsA
VITE_CLOUDINARY_CLOUD_NAME=dwlh2suxj
VITE_CLOUDINARY_UPLOAD_PRESET=fattoria_upload
```

### 2. Deploy in Produzione

Quando deployi il frontend su una piattaforma come Vercel o Netlify, dovrai configurare le variabili d'ambiente nel loro pannello di controllo.

1.  **Ottieni l'URL del Backend**: Prendi l'URL pubblico fornito da Google Cloud Run (es. `https://...a.run.app`).

2.  **Imposta le Variabili d'Ambiente sulla Piattaforma di Hosting**:
    *   `VITE_API_BASE_URL`: L'URL del tuo backend di produzione (es. `https://app-name-xyz-ew.a.run.app/api`).
    *   `VITE_STRIPE_PUBLIC_KEY`: La tua chiave **pubblica LIVE** di Stripe (es. `pk_live_...`).
    *   `VITE_CLOUDINARY_CLOUD_NAME`: Il tuo cloud name di Cloudinary.
    *   `VITE_CLOUDINARY_UPLOAD_PRESET`: Il tuo preset di upload di Cloudinary.

3.  **Verifica CORS**: Assicurati che l'URL dove è ospitato il tuo frontend (es. `https://www.il-tuo-sito.com`) sia esattamente quello che hai impostato nella variabile `CORS_ALLOWED_ORIGINS` del backend.
