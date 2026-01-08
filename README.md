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
- **Apache POI**: Per la generazione di file Excel (`.xlsx`).
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

- **Iscrizione Pubblica**: Fornisce un endpoint pubblico che permette agli utenti di iscriversi alla newsletter. 
- **Gestione Amministrativa**: L'amministratore ha a disposizione endpoint protetti per aggiungere o rimuovere iscritti manualmente.
- **Invio Massivo**: Funzionalità per inviare comunicazioni a tutti gli iscritti tramite Brevo.

### 8. Pannello di Amministrazione

- **Dashboard**: Area riservata per monitorare lo stato dell'applicazione, gestire impostazioni e visualizzare statistiche di vendita e di Vercel Analytics.

#### Nota Tecnica: Caricamento Ottimizzato delle Spedizioni

Per garantire la massima performance nel pannello di amministrazione, la lista delle spedizioni (`/api/admin/shipments/list`) viene caricata con una strategia ottimizzata che previene il "problema delle query N+1", riducendo a 3 le chiamate al database indipendentemente dal numero di spedizioni.

**Requisito Fondamentale:** Questa ottimizzazione richiede un **indice composito** in Firestore. Se l'indice non esiste, la prima chiamata all'API genererà un errore `FAILED_PRECONDITION` nei log, contenente un link per creare l'indice con un click.

### 9. Generazione Report Excel Avanzati

Il pannello di amministrazione offre una potente funzionalità di reporting che consente di esportare un'analisi dettagliata del business in formato Excel. Questo strumento è essenziale per la contabilità, l'analisi delle vendite e il monitoraggio dell'inventario.

- **Endpoint**: `GET /api/admin/reports/excel`
- **Autenticazione**: Richiede privilegi di amministratore.
- **Parametri di Query**:
  - `startDate`: La data di inizio del periodo di reporting (formato `YYYY-MM-DD`).
  - `endDate`: La data di fine del periodo di reporting (formato `YYYY-MM-DD`).

#### Struttura del Report

Il file Excel generato contiene tre fogli di lavoro, ciascuno progettato per offrire una prospettiva diversa sul business:

1.  **Riepilogo Mensile**:
    Fornisce una visione d'insieme delle performance finanziarie nel periodo selezionato.
    - **Entrate Totali Nette**: La somma totale pagata dai clienti (`subtotal` dell'ordine padre).
    - **Valore Prodotti**: Il ricavo generato dalla sola vendita dei prodotti (`Entrate Totali Nette` - `Costi di Spedizione Incassati`).
    - **Costi di Spedizione Incassati**: La somma di tutte le spese di spedizione pagate dai clienti.
    - **Valore Totale Sconti**: La somma di tutti gli sconti applicati a livello di ordine.
    - Altre statistiche includono: `Numero Clienti Unici`, `Numero Ordini`, `Numero Spedizioni Generate` e `Valore Medio Ordine`.

2.  **Vendite per Prodotto**:
    Aggrega i dati di vendita per ogni singolo prodotto, offrendo una chiara visione di quali articoli performano meglio.
    - **Nome Prodotto**: Il nome dell'articolo.
    - **Quantità Totale Venduta**: Il numero totale di unità vendute per quel prodotto nel periodo.
    - **Ricavo Generato**: Il ricavo totale generato dal prodotto, calcolato utilizzando il prezzo finale di vendita effettivo.

3.  **Elenco Ordini**:
    Offre una vista granulare e dettagliata di ogni singolo articolo all'interno di ogni spedizione.
    - **Dati Anagrafici**: Include ID ordine, data, cliente ed email.
    - **Dettagli Spedizione**: Mostra l'ID della spedizione (`child_...`) e il suo stato.
    - **Dettagli Articolo**: Per ogni articolo vengono mostrati:
        - **Prezzo Originale**: Il prezzo di listino (`price`).
        - **Prezzo Finale**: Il prezzo effettivo pagato dal cliente. **Questo valore viene letto dal campo `discountPrice` se presente; in caso contrario, corrisponde al prezzo originale.** Questa logica assicura una contabilità precisa anche in presenza di promozioni specifiche sul prodotto.

## Chiamate alle API

(La sezione delle API è stata omessa per brevità, ma è presente nel file originale)

## Configurazione

Assicurarsi di configurare le variabili d'ambiente o il file `application.properties` con le credenziali per Firebase, Stripe, Brevo e Vercel.

## Come Eseguire il Progetto

1.  Clona il repository.
2.  Configura le credenziali.
3.  Esegui con Maven: `./mvnw spring-boot:run`

L'applicazione sarà disponibile su `http://localhost:8080`.
