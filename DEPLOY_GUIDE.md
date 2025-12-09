# Guida Completa al Deploy su Google Cloud Run

Questa guida documenta il processo **definitivo e testato** per effettuare il deploy del backend dell'Azienda Agricola su Google Cloud Run. Seguire questi passaggi assicura un deploy pulito, ripetibile e senza errori.

---

<a id="concetto-chiave"></a>
## Concetto Chiave: File di Configurazione vs. Riga di Comando

Invece di passare le credenziali e le variabili d'ambiente come una stringa di testo lunghissima e soggetta a errori nel terminale, utilizziamo due file per definire la nostra configurazione in modo pulito e robusto:

1.  **`Dockerfile`**: Definisce l'ambiente di esecuzione del container, inclusi i parametri di avvio della Java Virtual Machine (JVM).
2.  **`env.yaml`**: Contiene tutte le chiavi segrete e le variabili di configurazione dell'applicazione (API keys, credenziali, etc.).

Questo approccio centralizza la configurazione e la separa dal codice dell'applicazione.

---

## Prerequisiti

- Aver installato e configurato `gcloud` CLI sul proprio computer.
- Essere autenticati con l'account Google corretto (`gcloud auth login`).
- Trovarsi con il terminale nella cartella principale del progetto (`AziendaAgricolaBE`).

---

## Fase 1: Verificare il `Dockerfile`

Il nostro backend Java richiede specifiche opzioni di avvio della JVM per funzionare correttamente nell'ambiente containerizzato di Cloud Run. È fondamentale che questi "flag" siano presenti nel `Dockerfile`.

Assicurati che la riga `ENTRYPOINT` nel tuo `Dockerfile` sia esattamente così:

```dockerfile
# Comando per avviare l'applicazione con tutte le correzioni necessarie per la JVM
ENTRYPOINT ["java", "-Dserver.port=${PORT:8080}", "--add-opens", "java.base/java.time=ALL-UNNAMED", "--add-opens", "java.base/java.time.chrono=ALL-UNNAMED", "--add-opens", "java.base/java.nio=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/usr/local/lib/app.jar"]
```

> **Lezione Appresa**: Centralizzare tutti i flag `--add-opens` nel `Dockerfile` rende il container auto-configurante e indipendente da variabili d'ambiente esterne come `JAVA_TOOL_OPTIONS`, riducendo il rischio di conflitti.

---

## Fase 2: Creare il File `env.yaml`

Questo file conterrà tutte le variabili d'ambiente necessarie al backend.

1.  Nella cartella principale del progetto (`AziendaAgricolaBE`), crea un nuovo file e chiamalo esattamente **`env.yaml`**.

2.  Copia e incolla il seguente contenuto al suo interno. Questo template è già configurato con la sintassi corretta.

```yaml
# -----------------------------------------------------------------------------
# File per le variabili d'ambiente di Google Cloud Run.
# Versione DEFINITIVA con la corretta formattazione YAML per la chiave privata.
# NON EFFETTUARE IL COMMIT DI QUESTO FILE SU GIT.
# -----------------------------------------------------------------------------

# Variabili per servizi esterni (Stripe, Brevo, Cloudinary, Vercel)
# SOSTITUIRE I SEGNAPOSTO CON LE PROPRIE CHIAVI SEGRETE
BREVO_API_KEY: 'LA_TUA_CHIAVE_API_BREVO'
BREVO_SENDER_EMAIL: 'la_tua_email_mittente@esempio.com'
BREVO_API_URL: 'https://api.brevo.com/v3/smtp/email'
CLOUDINARY_CLOUD_NAME: 'IL_TUO_CLOUD_NAME'
CLOUDINARY_API_KEY: 'LA_TUA_CHIAVE_API_CLOUDINARY'
CLOUDINARY_API_SECRET: 'IL_TUO_API_SECRET_CLOUDINARY'
STRIPE_SECRET_KEY: 'la_tua_chiave_segreta_stripe (sk_live_... o sk_test_...)'
VERCEL_API_TOKEN: 'IL_TUO_TOKEN_API_VERCEL'
VERCEL_PROJECT_ID: 'IL_TUO_PROJECT_ID_VERCEL'

# Variabili di configurazione dell'applicazione
GOOGLE_CLOUD_PROJECT: 'il_tuo_project_id_gcp'
CORS_ALLOWED_ORIGINS: '*' # o un dominio specifico
ADMIN_USERNAME: 'il_tuo_username_admin'
ADMIN_PASSWORD: 'la_tua_password_admin'

# Credenziali Firebase in formato JSON.
# L'uso del | (literal block) garantisce che i caratteri di a-capo (\n) nella chiave privata siano conservati correttamente.
# INCOLLARE QUI IL CONTENUTO DEL FILE JSON DEL SERVICE ACCOUNT
GOOGLE_CREDENTIALS_JSON: |
  {
    "type": "service_account",
    "project_id": "IL_TUO_PROJECT_ID_FIREBASE",
    "private_key_id": "...",
    "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
    "client_email": "...",
    "client_id": "...",
    "auth_uri": "https.accounts.google.com/o/oauth2/auth",
    "token_uri": "https.oauth2.googleapis.com/token",
    "auth_provider_x509_cert_url": "https.www.googleapis.com/oauth2/v1/certs",
    "client_x509_cert_url": "..."
  }

```

> **Lezione Appresa**: La chiave privata di Firebase contiene dei caratteri "a capo" (`
`). La sintassi YAML standard (`'...'`) non interpreta questi caratteri, corrompendo la chiave. Usando `|` (chiamato "literal block scalar"), diciamo a YAML di preservare la stringa esattamente com'è, inclusi gli "a capo", risolvendo l'errore `missing end tag`.

---

## Fase 3: Eseguire il Deploy

Una volta che il `Dockerfile` è verificato e il file `env.yaml` è stato creato, sei pronto per il deploy.

Esegui questo singolo comando dal tuo terminale:

```bash
gcloud run deploy azienda-agricola-backend --source . --region europe-west1 --project base-be-azienda --allow-unauthenticated --env-vars-file=env.yaml
```

Il processo di build e deploy richiederà qualche minuto. Al termine, `gcloud` ti fornirà l'URL pubblico del tuo servizio.

---

## Appendice: Troubleshooting Futuro

Se un deploy dovesse fallire di nuovo con l'errore "Container failed to start", il primo passo è sempre **leggere i log**. Il comando di deploy stesso ti fornirà un URL per i log della revisione fallita.

In alternativa, puoi usare `gcloud` per recuperare i log.

1.  Trova il nome della revisione fallita (es: `azienda-agricola-backend-00021-zqc`) dal messaggio di errore.
2.  Esegui questo comando, sostituendo il nome della revisione e adattando le virgolette per il tuo terminale (Windows `cmd.exe` usa le doppie virgolette esterne).

    **Comando per Windows (cmd.exe):**
    ```bash
    gcloud logging read "resource.type=\"cloud_run_revision\" AND resource.labels.service_name=\"azienda-agricola-backend\" AND resource.labels.revision_name=\"NOME_REVISIONE_FALLITA\"" --project base-be-azienda --limit 20
    ```

Cerca nei log un `FATAL EXCEPTION` o un `Application run failed`. L'errore Java ti dirà esattamente cosa non ha funzionato all'avvio.
