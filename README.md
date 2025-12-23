# Progetto Spring Boot con JWT, Firebase e Stripe

Questo progetto è un'applicazione Java basata su Spring Boot che fornisce un backend completo per la gestione di prodotti, ordini, newsletter, impostazioni di sistema e un pannello di amministrazione. L'applicazione integra l'autenticazione basata su token JWT, la connettività a Firebase, l'elaborazione dei pagamenti con Stripe, l'invio di email transazionali con Brevo e il monitoraggio delle statistiche del sito tramite Vercel Analytics.

## Tecnologie Utilizzate

- **Spring Boot 3.2.1**: Framework per la creazione di applicazioni Java stand-alone.
- **Spring Security**: Per la gestione dell'autenticazione e autorizzazione.
- **JWT (JSON Web Token)**: Per la creazione di token di accesso per un'autenticazione sicura.
- **Firebase Admin SDK**: Per l'integrazione con i servizi di Firebase.
- **Stripe-Java**: Libreria ufficiale di Stripe per l'elaborazione dei pagamenti in Java.
- **Brevo API**: Per l'invio di email transazionali.
- **Thymeleaf**: Per la gestione di template HTML dinamici, in particolare per le email.
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

### 4. Comunicazioni via Email (Brevo)

#### Conferma d'Ordine
Al completamento di un acquisto, il sistema invia un'email di conferma al cliente.
- **Template HTML Dinamici**: Le email sono basate su template HTML dinamici gestiti da **Thymeleaf**.
- **Dettaglio Completo**: L'email contiene un riepilogo dell'ordine, suddiviso per spedizioni (figli) con il relativo stato e gli articoli.

#### Notifica di Spedizione con Tracking
Quando un amministratore aggiorna lo stato di una spedizione a "Spedito" e inserisce il numero di tracciamento:
- **Invio Automatico**: Il sistema invia automaticamente un'email al cliente per notificargli che la spedizione è partita.
- **Chiarezza per il Cliente**: L'email si concentra esclusivamente sulla spedizione specifica (ordine "figlio"), mostrando l'**ID della spedizione** e il **numero di tracciamento**.
- **Contenuto Essenziale**: Include la lista degli articoli presenti in quella specifica spedizione, evitando confusione nel caso di ordini suddivisi.

### 5. Gestione degli Ordini con Logica Padre-Figlio

- **Struttura Padre-Figlio**: Un acquisto genera un ordine **"padre"** (dati anagrafici e finanziari) e uno o più ordini **"figlio"** (le singole spedizioni).
- **Separazione Spedizioni**: La logica separa automaticamente gli articoli in pre-ordine da quelli disponibili, creando spedizioni multiple se richiesto.
- **Integrazione con Stripe**: Utilizza Stripe per elaborare i pagamenti a livello di ordine "padre".


### 6. Impostazioni di Sistema

- **Configurazione Dinamica**: API per gestire le impostazioni chiave (es. costi di spedizione).
- **Endpoint Pubblico e Privato**: Un endpoint pubblico per la lettura e uno protetto per la modifica.

### 7. Gestione della Newsletter

- **Iscrizione Pubblica**: Fornisce un endpoint pubblico che permette agli utenti di iscriversi alla newsletter. Il sistema è progettato per gestire in modo silenzioso le iscrizioni duplicate: se un utente prova a iscriversi con un'email già presente, la richiesta viene accettata senza creare un nuovo record, garantendo un'esperienza utente fluida.
- **Gestione Amministrativa**: L'amministratore ha a disposizione endpoint protetti per aggiungere o rimuovere iscritti manualmente. Questo permette una gestione completa della lista di contatti.
- **Invio Massivo**: Funzionalità per inviare comunicazioni a tutti gli iscritti tramite Brevo.

### 8. Pannello di Amministrazione

- **Dashboard**: Area riservata per monitorare lo stato dell'applicazione, gestire impostazioni e visualizzare statistiche di vendita e di Vercel Analytics.

#### Nota Tecnica: Caricamento Ottimizzato delle Spedizioni

Per garantire la massima performance nel pannello di amministrazione, la lista delle spedizioni (`/api/admin/shipments/list`) viene caricata con una strategia ottimizzata che previene il "problema delle query N+1".

**Funzionamento:**
1.  **Query Unica per le Spedizioni**: Il sistema recupera tutte le spedizioni attive (ordini `CHILD` non consegnati) con una singola query.
2.  **Aggregazione degli ID**: Dal risultato, vengono estratti tutti gli ID unici degli ordini "padre" e dei prodotti associati.
3.  **Query Massive (Bulk Fetching)**: Vengono eseguite solo altre due query, utilizzando la clausola `IN`, per recuperare in un colpo solo tutti i dati anagrafici degli ordini "padre" e tutti i dettagli dei prodotti necessari.
4.  **Join in Memoria**: I dati delle tre query vengono uniti nell'applicazione Java per costruire l'oggetto finale (`ShipmentListDTO`) da inviare al frontend.

Questo approccio riduce il numero di chiamate al database da N (una per ogni spedizione) a un totale fisso di 3, migliorando drasticamente i tempi di caricamento.

**Requisito Fondamentale: Indice Composito in Firestore**

Questa strategia di query richiede obbligatoriamente un **indice composito** in Firestore per poter filtrare le spedizioni in modo efficiente.

**Azione Richiesta:** Se l'indice non esiste, la prima volta che si esegue la chiamata API il backend genererà un errore `FAILED_PRECONDITION` nei log. **Questo è un comportamento atteso.** Il messaggio di errore conterrà un link. È sufficiente **cliccare su quel link** per essere reindirizzati alla console di Firebase, dove si potrà creare l'indice con un solo click. Una volta che l'indice è stato creato (richiede qualche minuto), la funzionalità opererà correttamente.

## Struttura del Progetto

Il progetto è organizzato nei seguenti package principali: `config`, `security`, `admin`, `newsletter`, `order`, `product`, `settings`.

## Chiamate alle API

### Prodotti (`/api/products`)

- `GET /api/products`: Recupera la lista dei prodotti. (Pubblico)
- `POST /api/products`: Crea un nuovo prodotto. (Admin)
- ... (altri endpoint CRUD)

### Impostazioni

- `GET /api/settings/public`: Recupera le impostazioni pubbliche. (Pubblico)
- `PUT /api/admin/settings`: Aggiorna le impostazioni. (Admin)

### Ordini e Spedizioni

Il flusso di creazione è in due fasi:

1.  **`POST /api/orders/charge`**: (Autorizzazione) Verifica stock e totale, poi crea un `PaymentIntent` con Stripe.
    - **Risposta**: `clientSecret` per confermare il pagamento sul frontend.
2.  **`POST /api/orders/create`**: (Salvataggio) Da chiamare **dopo** la conferma del pagamento sul frontend. Salva l'ordine nel database e invia l'email di conferma.
    - **Risposta**: Conferma della creazione.

- `GET /api/orders`: Recupera la lista degli ordini "padre". (Admin)
- `GET /api/orders/{id}`: Recupera i dettagli di un ordine padre e delle sue spedizioni (figli). (Admin)
- `PUT /api/shipments/{id}/status`: Aggiorna lo stato di una singola spedizione (ordine "figlio"). (Admin)
- ... (altri endpoint di gestione)

### Newsletter

#### Endpoint Pubblici (`/api/newsletter`)

- `POST /api/newsletter/subscribe`: Iscrive un nuovo utente alla newsletter.
  - **Body**: `{ "email": "utente@esempio.com" }`
  - **Risposta**: `200 OK` sia per le nuove iscrizioni sia se l'email è già presente.

#### Endpoint di Amministrazione (`/api/admin/newsletter`)

- `POST /api/admin/newsletter/subscribe`: Aggiunge un nuovo iscritto (richiede autenticazione admin).
  - **Body**: `{ "email": "nuovoutente@esempio.com" }`
  - **Risposta**: `200 OK`, gestisce i duplicati senza errori.

- `POST /api/admin/newsletter/unsubscribe`: Rimuove un iscritto dalla newsletter (richiede autenticazione admin).
  - **Body**: `{ "email": "utente@esempio.com" }`
  - **Risposta**: `200 OK` se l'utente viene rimosso o se non era presente.

- `POST /api/admin/newsletter/send`: Invia la newsletter a tutti gli iscritti.
  - **Body**: `{ "subject": "Oggetto della mail", "message": "Contenuto HTML della mail" }`

- `GET /api/admin/newsletter/subscribers/count`: Ottiene il numero totale di iscritti.


## Configurazione

Assicurarsi di configurare le variabili d'ambiente o il file `application.properties` con le credenziali per Firebase, Stripe, Brevo e Vercel.

## Come Eseguire il Progetto

1.  Clona il repository.
2.  Configura le credenziali.
3.  Esegui con Maven: `./mvnw spring-boot:run`

L'applicazione sarà disponibile su `http://localhost:8080`.
