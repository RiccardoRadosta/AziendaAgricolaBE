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
- **Confronto con Tolleranza**: Il totale calcolato dal server viene confrontato con quello inviato dal client. Per gestire le piccole e inevitabili imprecisioni dei calcoli in virgola mobile (comuni in JavaScript), il sistema accetta la transazione solo se la differenza assoluta tra i due totali è inferiore a una soglia minima (es. 0.01 EUR). Se la discrepanza è maggiore, la transazione viene rifiutata con un errore `409 Conflict`.

### 3. Gestione Prodotti

- **CRUD Completo**: API per creare, leggere, aggiornare ed eliminare i prodotti del catalogo.
- **Sicurezza a livello di endpoint**: Le operazioni di modifica sono protette e richiedono autenticazione, mentre la lettura dei prodotti è pubblica.

### 4. Gestione degli Ordini con Logica Padre-Figlio

- **Struttura Padre-Figlio**: Un singolo acquisto del cliente genera un ordine **"padre"** che contiene i dati anagrafici, di contatto e finanziari totali. L'ordine padre è collegato a uno o più ordini **"figlio"**, che rappresentano le singole spedizioni.
- **Separazione Spedizioni**: La logica di business separa automaticamente gli articoli in pre-ordine da quelli subito disponibili. Se l'utente sceglie l'opzione `split`, vengono creati ordini "figlio" multipli, permettendo una gestione indipendente dello stato di ogni spedizione.
- **Integrazione con Stripe**: Utilizza Stripe per elaborare i pagamenti a livello di ordine "padre".

### 5. Impostazioni di Sistema

- **Configurazione Dinamica**: API per gestire le impostazioni chiave dell'applicazione, come i costi di spedizione.
- **Endpoint Pubblico e Privato**: Un endpoint pubblico per la lettura delle impostazioni da parte del frontend e un endpoint di amministrazione protetto per la loro modifica.
- **Robustezza**: L'endpoint pubblico fornisce valori di default per garantire che il frontend riceva sempre dati validi, anche se le impostazioni non sono state configurate.

### 6. Iscrizione alla Newsletter

- **API per l'Iscrizione**: Fornisce endpoint per consentire agli utenti di iscriversi e annullare l'iscrizione.

### 7. Pannello di Amministrazione

- **Dashboard**: Un'area riservata per monitorare lo stato dell'applicazione.
- **Gestione Impostazioni**: Permette di configurare i costi di spedizione (standard, soglia gratuita, spedizione divisa).
- **Statistiche**: Fornisce dati aggregati e i prodotti più venduti.
- **Visualizzazione Analytics**: Integra le statistiche di Vercel Analytics.

## Struttura del Progetto

Il progetto è organizzato nei seguenti package:

- `config`: Classi di configurazione per Firebase, Stripe, etc.
- `security`: Gestione della sicurezza e dei token JWT.
- `admin`: Controller e servizi per il pannello di amministrazione.
- `newsletter`: Logica per le iscrizioni alla newsletter.
- `order`: Gestione degli ordini secondo la logica padre-figlio.
- `product`: Gestione dei prodotti del catalogo (CRUD).
- `settings`: Servizi e controller per la gestione delle impostazioni di sistema.

## Chiamate alle API

### Prodotti (`/api/products`)

- `GET /api/products`: Recupera la lista di tutti i prodotti. (Pubblico)
- `GET /api/products/{id}`: Recupera i dettagli di un singolo prodotto. (Pubblico)
- `POST /api/products`: Crea un nuovo prodotto. (Admin)
- `PUT /api/products/{id}`: Aggiorna un prodotto esistente. (Admin)
- `DELETE /api/products/{id}`: Elimina un prodotto. (Admin)

### Impostazioni (`/api/settings` e `/api/admin/settings`)

- `GET /api/settings/public`: Recupera le impostazioni pubbliche del sistema, come i costi di spedizione. Restituisce valori di default se non configurate. (Pubblico)
- `PUT /api/admin/settings`: Aggiorna le impostazioni di sistema. (Admin)
    - **Body**: Un oggetto JSON con le impostazioni da aggiornare (es. `{ "standardShippingCost": 35, "splitShippingCost": 10 }`).

### Ordini e Spedizioni

Il flusso di creazione di un ordine è separato in due fasi: pagamento e salvataggio.

1.  **`POST /api/orders/charge`**: (Prima fase)
    Crea un `PaymentIntent` con Stripe per autorizzare il pagamento. Verifica la disponibilità di magazzino e il totale dell'ordine prima di procedere.
    - **Body**: `OrderDTO` contenente i dettagli dell'ordine e il `paymentToken` di Stripe.
    - **Risposta**: `clientSecret` per confermare il pagamento sul frontend.

2.  **`POST /api/orders/create`**: (Seconda fase)
    Da chiamare **dopo** che il pagamento è stato confermato con successo sul frontend. Salva l'ordine (padre e figli) nel database.
    - **Body**: `OrderDTO` completo.
    - **Risposta**: Conferma della creazione dell'ordine.

- `GET /api/orders`: Recupera la lista di tutti gli ordini "padre". Utile per una visione d'insieme nel pannello admin. (Admin)
- `GET /api/orders/{id}`: Recupera i dettagli completi di un ordine.
  - **Parametro**: `id` dell'ordine **padre**.
  - **Risposta**: Un oggetto JSON contenente `parent` (i dati dell'ordine padre) e `shipments` (una lista degli ordini figlio associati). (Admin)
- `PUT /api/orders/{id}`: Aggiorna i dati anagrafici e di contatto di un ordine "padre". (Admin)
- `PUT /api/shipments/{id}/status`: Aggiorna lo stato di una singola spedizione (un ordine "figlio").
  - **Parametro**: `id` dell'ordine **figlio**.
  - **Body**: `{ "status": <new_status_code> }` (es. `{ "status": 2 }`). (Admin)
- `DELETE /api/orders/{id}`: Elimina un ordine "padre" e tutti i suoi "figli" (spedizioni) collegati. (Admin)

(Altre sezioni API omesse per brevità)

## Configurazione

Assicurarsi di configurare le variabili d'ambiente o il file `application.properties` con le credenziali per Firebase, Stripe, Brevo e Vercel come indicato nella documentazione precedente e nei file di esempio.

## Come Eseguire il Progetto

1.  **Clona il repository.**
2.  **Configura le credenziali.**
3.  **Compila ed esegui** con Maven:
    ```bash
    ./mvnw spring-boot:run
    ```
L'applicazione sarà disponibile all'indirizzo `http://localhost:8080`.
