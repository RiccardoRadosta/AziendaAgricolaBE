# Backend E-commerce con Spring Boot

## Riepilogo dell'Applicazione

Questa è un'applicazione backend basata su Java e il framework Spring Boot, progettata per servire un e-commerce. Fornisce le API necessarie per gestire la creazione di ordini e le iscrizioni alla newsletter.

### Funzionalità Principali

1.  **Creazione Ordini:** Riceve i dettagli di un ordine dal frontend, li valida, li salva su un database e restituisce una conferma.
2.  **Iscrizione Newsletter:** Salva l'email di un utente nel database per future comunicazioni.

---

## Struttura del Database

La struttura del database è gestita tramite Spring Data JPA e viene creata automaticamente a partire dalle classi `@Entity` del codice.

-   **Tabella `customer_order`**: Contiene tutti i dettagli degli ordini dei clienti.
-   **Tabella `newsletter_subscription`**: Archivia gli indirizzi email degli utenti iscritti alla newsletter.

---

## Guida all'Avvio e al Test

Per testare l'applicazione, è necessario avviare il server web integrato in locale. Questo renderà gli endpoint API accessibili per un client frontend.

### Come Avviare il Server

Apri un terminale nella cartella principale del progetto ed esegui il seguente comando:

```bash
./mvnw spring-boot:run
```

Questo comando avvierà l'applicazione. Se tutto va a buon fine, vedrai nel log del terminale che il server è in ascolto sulla porta `8080`.

---

## Documentazione API (Endpoint per il Frontend)

Una volta che il server è in esecuzione, i seguenti endpoint saranno disponibili per il test.

### 1. Creare un Nuovo Ordine

-   **URL:** `http://localhost:8080/api/orders/create`
-   **Metodo HTTP:** `POST`
-   **Headers:** `Content-Type: application/json`
-   **Corpo della Richiesta (Payload JSON):**

    ```json
    {
        "fullName": "Mario Rossi",
        "email": "mario.rossi@example.com",
        "phone": "3331234567",
        "address": "Via Roma 10",
        "city": "Milano",
        "province": "MI",
        "postalCode": "20121",
        "country": "Italia",
        "newsletterSubscribed": true,
        "orderNotes": "Consegnare al portiere se non sono in casa.",
        "items": "[{\"productName\": \"T-Shirt Nera\", \"size\": \"L\", \"quantity\": 1}]",
        "subtotal": 89.99
    }
    ```

-   **Risposta di Successo (HTTP 201 Created):**

    ```json
    {
        "status": "Order created successfully"
    }
    ```

### 2. Iscriversi alla Newsletter

-   **URL:** `http://localhost:8080/api/newsletter/subscribe`
-   **Metodo HTTP:** `POST`
-   **Headers:** `Content-Type: application/json`
-   **Corpo della Richiesta (Payload JSON):**

    ```json
    {
        "email": "nuovo.iscritto@email.com"
    }
    ```

-   **Risposta di Successo (HTTP 200 OK):**

    ```json
    {
        "message": "Subscription successful!"
    }
    ```
