# Guida al Deploy del Frontend su Vercel

Questa guida documenta i passaggi per effettuare il deploy del frontend (React + Vite) dell'Azienda Agricola su Vercel, includendo la risoluzione degli errori più comuni riscontrati.

---

## Concetto Chiave: SPA su Hosting Statico

Un'applicazione React come la nostra è una "Single Page Application" (SPA). Ciò significa che un unico file `index.html` è il punto di ingresso per tutte le pagine. La navigazione tra le pagine (es. da `/` a `/admin`) è gestita lato client da React Router.

Un servizio di hosting statico come Vercel, per impostazione predefinita, si aspetta che ogni URL corrisponda a un file o a una cartella fisica. Questo causa problemi quando si tenta di accedere direttamente a un URL come `iltuosito.com/admin`, generando un errore 404. La soluzione è configurare Vercel per reindirizzare tutte le richieste al file `index.html`, lasciando che React Router gestisca la navigazione.

---

## Prerequisiti

1.  **Account Vercel**: Un account Vercel, preferibilmente collegato al proprio account GitHub.
2.  **Progetto su GitHub**: Il codice del frontend deve essere pushato su un repository GitHub.
3.  **Backend Online**: L'URL del backend deployato deve essere noto, in quanto servirà come variabile d'ambiente.

---

## Procedura di Deploy

### 1. Importazione del Progetto

1.  Accedi alla tua dashboard Vercel.
2.  Clicca su "Add New..." -> "Project".
3.  Seleziona il repository GitHub del tuo frontend (`AziendaAgricola_LandingPage`) e clicca su "Import".

### 2. Configurazione del Progetto

Vercel ti porterà a una schermata di configurazione.

1.  **Framework Preset**: Assicurati che Vercel rilevi automaticamente il progetto come **Vite**.
2.  **Environment Variables (Variabili d'Ambiente)**: Questa è la fase più critica. Nella sezione "Environment Variables", aggiungi tutte le chiavi che hai nel tuo file `.env.local`. Devono iniziare con `VITE_`.
    - `VITE_API_BASE_URL`: L'URL completo del tuo backend (es: `https://...run.app/api`).
    - `VITE_STRIPE_PUBLIC_KEY`: La chiave pubblica di Stripe.
    - `VITE_CLOUDINARY_CLOUD_NAME`: Il nome del cloud di Cloudinary.
    - `VITE_CLOUDINARY_UPLOAD_PRESET`: Il preset di upload di Cloudinary.

### 3. Esecuzione del Deploy

- Clicca sul pulsante **"Deploy"**. Vercel installerà le dipendenze, eseguirà il build del progetto e lo pubblicherà.

---

## Troubleshooting: Risoluzione degli Errori Comuni

### Errore 1: La Build Fallisce a Causa di `vite.config.ts`

- **Sintomo**: L'errore nei log di Vercel è `error TS6305: Output file '/vercel/path0/vite.config.d.ts' has not been built from source file '/vercel/path0/vite.config.ts'`.
- **Causa**: Il file `tsconfig.json` è troppo generico e include anche il file di configurazione di Vite (`vite.config.ts`) nel processo di compilazione di TypeScript, cosa non corretta.
- **Soluzione**: Aprire il file `tsconfig.json` e aggiungere `vite.config.ts` all'array `"exclude"`.
  ```json
  "exclude": ["node_modules", "dist", "vite.config.ts"]
  ```

### Errore 2: La Build Fallisce per Variabili Non Utilizzate

- **Sintomo**: L'errore nei log è `error TS6133: '...' is declared but its value is never read`.
- **Causa**: Il progetto è configurato con regole di linting strette (`"strict": true` in `tsconfig.json`), che trattano le variabili, gli import o i parametri di funzione non utilizzati come errori che bloccano la build.
- **Soluzione Rapida**: Modificare `tsconfig.json` per dire a TypeScript di ignorare questi casi impostando le seguenti opzioni su `false`:
  ```json
  "compilerOptions": {
    // ...
    "noUnusedLocals": false,
    "noUnusedParameters": false
    // ...
  }
  ```
- **Soluzione Ideale (a lungo termine)**: Rimuovere dal codice tutti gli import e le variabili che non vengono effettivamente utilizzate.

### Errore 3: Errore 404 Navigando Direttamente a una Rotta (es. `/admin`)

- **Sintomo**: La homepage funziona, ma ricaricare una pagina interna o accedervi tramite URL diretto restituisce un errore 404 di Vercel.
- **Causa**: Come spiegato nel "Concetto Chiave", Vercel non sa come gestire le rotte lato client di una SPA.
- **Soluzione**: Creare un file `vercel.json` nella root del progetto con la seguente regola di "rewrite". Questo file istruisce Vercel a servire sempre `index.html` per qualsiasi percorso non trovi, passando il controllo a React Router.
  ```json
  {
    "rewrites": [
      {
        "source": "/(.*)",
        "destination": "/index.html"
      }
    ]
  }
  ```
  **Nota**: Questo file non ha alcun effetto sull'ambiente di sviluppo locale.

---

## Deploy di Produzione

Il deploy sul sito live **NON è più automatico** ad ogni push su `main`.

L'ambiente di produzione su Vercel è ora collegato a un branch dedicato (es. `produzione`).

Il flusso di lavoro corretto per pubblicare le modifiche è:
1.  Assicurarsi che tutte le modifiche desiderate siano state testate e unite al branch `main`.
2.  Dal proprio terminale locale, aggiornare e unire `main` nel branch di produzione:
    ```bash
    # Passa al branch di produzione
    git checkout produzione

    # Assicurati che sia aggiornato con la versione remota
    git pull origin produzione

    # Unisci le ultime modifiche da main
    git merge main

    # Fai il push del branch di produzione su GitHub
    git push origin produzione
    ```
3.  Solo quest'ultimo push (`git push origin produzione`) attiverà il deploy sul sito di produzione.
