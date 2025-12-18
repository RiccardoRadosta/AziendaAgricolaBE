# Progetto Spring Boot con JWT, Firebase e Stripe

Questo progetto è un'applicazione Java basata su Spring Boot che fornisce un backend completo per la gestione di prodotti, ordini, newsletter, impostazioni di sistema e un pannello di amministrazione. L'applicazione integra l'autenticazione basata su token JWT, la connettività a Firebase, l'elaborazione dei pagamenti con Stripe e il monitoraggio delle statistiche del sito tramite Vercel Analytics.

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

- **Autenticazione JWT**: L'accesso alle API di amministrazione è protetto tramite token JWT.
- **Filtro di Sicurezza**: Un `JwtRequestFilter` intercetta ogni richiesta per validare il token JWT.

### 2. Politica di Sicurezza per i Pagamenti

Per garantire la massima sicurezza e prevenire discrepanze, il sistema adotta una politica di verifica a due fasi per ogni transazione:

- **Calcolo Autoritativo del Totale sul Backend**: Il frontend invia i dettagli dell'ordine, ma il calcolo finale del totale viene sempre eseguito sul backend. Questo previene manipolazioni del prezzo dal lato client.
- **Ordine di Calcolo Stretto**: I benefici basati sul totale, come la **spedizione gratuita**, vengono calcolati sul subtotale finale della merce, **dopo** l'applicazione di qualsiasi sconto o coupon. Questo garantisce che le offerte siano applicate solo quando il valore effettivo della vendita raggiunge la soglia di redditività.
- **Confronto con Tolleranza**: Il totale calcolato dal server viene confrontato con quello inviato dal client. Per gestire le piccole imprecisioni dei calcoli in virgola mobile, il sistema accetta la transazione solo se la differenza assoluta tra i due totali è inferiore a una soglia minima (es. 0.01 EUR). Se la discrepanza è maggiore, la transazione viene rifiutata con un errore `409 Conflict`.

### 3. Gestione Prodotti

- **CRUD Completo**: API per creare, leggere, aggiornare ed eliminare i prodotti del catalogo.
- **Sicurezza a livello di endpoint**: Le operazioni di modifica sono protette, mentre la lettura dei prodotti è pubblica.

### 4. Gestione degli Ordini con Logica Padre-Figlio

- **Struttura Padre-Figlio**: Un acquisto genera un ordine **"padre"** (dati anagrafici e finanziari) e uno o più ordini **"figlio"** (le singole spedizioni).
- **Separazione Spedizioni**: La logica separa automaticamente gli articoli in pre-ordine da quelli disponibili, creando spedizioni multiple se richiesto.
- **Integrazione con Stripe**: Utilizza Stripe per elaborare i pagamenti a livello di ordine "padre".

### 5. Impostazioni di Sistema

- **Configurazione Dinamica**: API per gestire le impostazioni chiave (es. costi di spedizione).
- **Endpoint Pubblico e Privato**: Un endpoint pubblico per la lettura e uno protetto per la modifica.

### 6. Iscrizione alla Newsletter

- **API per l'Iscrizione**: Fornisce endpoint per iscriversi e annullare l'iscrizione.
- **Controllo Anti-Duplicati**: Il sistema impedisce iscrizioni multiple. Se un utente si iscrive con un'email già presente, l'API risponde con un errore `409 Conflict`.

### 7. Pannello di Amministrazione

- **Dashboard**: Area riservata per monitorare lo stato dell'applicazione, gestire impostazioni e visualizzare statistiche di vendita e di Vercel Analytics.

## Struttura del Progetto

Il progetto è organizzato nei seguenti package principali: `config`, `security`, `admin`, `newsletter`, `order`, `product`, `settings`.

## Chiamate alle API

### Prodotti (`/api/products`)

- `GET /api/products`: Recupera la lista dei prodotti. (Pubblico)
- `POST /api/products`: Crea un nuovo prodotto. (Admin)
- ... (altri endpoint CRUD)

### Impostazioni (`/api/settings` e `/api/admin/settings`)

- `GET /api/settings/public`: Recupera le impostazioni pubbliche. (Pubblico)
- `PUT /api/admin/settings`: Aggiorna le impostazioni. (Admin)

### Ordini e Spedizioni

Il flusso di creazione è in due fasi:

1.  **`POST /api/orders/charge`**: (Autorizzazione) Verifica stock e totale, poi crea un `PaymentIntent` con Stripe.
    - **Risposta**: `clientSecret` per confermare il pagamento sul frontend.
2.  **`POST /api/orders/create`**: (Salvataggio) Da chiamare **dopo** la conferma del pagamento sul frontend. Salva l'ordine nel database.
    - **Risposta**: Conferma della creazione.

- `GET /api/orders`: Recupera la lista degli ordini "padre". (Admin)
- `GET /api/orders/{id}`: Recupera i dettagli di un ordine padre e delle sue spedizioni (figli). (Admin)
- `PUT /api/shipments/{id}/status`: Aggiorna lo stato di una singola spedizione (ordine "figlio"). (Admin)
- ... (altri endpoint di gestione)

### Newsletter (`/api/newsletter`)

- `POST /api/newsletter/subscribe`: Iscrive un nuovo utente alla newsletter.
  - **Body**: `{ "email": "utente@esempio.com" }`
  - **Risposta di successo**: Conferma dell'iscrizione.
  - **Risposta di errore**: `409 Conflict` se l'email è già iscritta.
- `DELETE /api/newsletter/unsubscribe`: Annulla l'iscrizione.
  - **Body**: `{ "email": "utente@esempio.com" }`

## Configurazione

Assicurarsi di configurare le variabili d'ambiente o il file `application.properties` con le credenziali per Firebase, Stripe, Brevo e Vercel.

## Come Eseguire il Progetto

1.  Clona il repository.
2.  Configura le credenziali.
3.  Esegui con Maven: `./mvnw spring-boot:run`

L'applicazione sarà disponibile su `http://localhost:8080`.
