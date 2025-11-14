# Backend E-commerce con Spring Boot e Stripe

## Riepilogo dell'Applicazione

Questa è un'applicazione backend basata su Java e il framework Spring Boot, progettata per servire un e-commerce. Integra il sistema di pagamento **Stripe** per processare gli addebiti in modo sicuro.

### Funzionalità Principali

1.  **Gestione Pagamenti e Ordini:** Riceve i dettagli di un ordine e un token di pagamento sicuro da Stripe, crea un `PaymentIntent` e restituisce lo stato al frontend per la conferma finale. Gestisce i flussi di autenticazione sicura (3D Secure).
2.  **Iscrizione Newsletter:** Salva l'email di un utente nel database per future comunicazioni.

---

## Prerequisiti

Prima di avviare l'applicazione, è necessario configurare la propria chiave segreta di Stripe.

1.  **Clona il repository.**
2.  **Apri il file `src/main/resources/application.properties`:**
3.  Trova la riga `#stripe.secret.key=`
4.  Rimuovi il commento (`#`) e incolla la tua chiave segreta di Stripe (es. `sk_test_...`).
5.  **Importante:** Questo file non deve essere "committato" con la chiave al suo interno.

## Avvio dell'Applicazione

Puoi avviare il server usando il Maven Wrapper incluso:

```bash
./mvnw spring-boot:run
```

Il server sarà in ascolto sulla porta `8080`.

---

## API Endpoints

L'applicazione espone i seguenti endpoint REST:

### 1. Creare un Intento di Pagamento

Questo endpoint crea un `PaymentIntent` di Stripe e restituisce il suo `clientSecret` e il suo stato iniziale al frontend. **Non finalizza il pagamento.**

-   **URL:** `http://localhost:8080/api/orders/charge`
-   **Metodo HTTP:** `POST`
-   **Corpo della Richiesta (Payload JSON):**
    ```json
    {
        "subtotal": 28.00,
        "paymentToken": "pm_xxxxxxxxxxxx",
        // ...altri dati dell'ordine...
    }
    ```

#### Risposte Possibili

-   **Risposta di Successo (HTTP 200 OK):** Indica che l'intento è stato creato. Il frontend deve ora usare il `clientSecret` per confermare il pagamento con Stripe.js.
    ```json
    {
        "status": "requires_confirmation",
        "clientSecret": "pi_xxxxxxxxxxxx_secret_xxxxxxxx"
    }
    ```
    Il frontend **deve** usare questo `clientSecret` per chiamare la funzione `stripe.confirmCardPayment()` di Stripe.js, la quale gestirà l'eventuale autenticazione 3D Secure e finalizzerà il pagamento.

-   **Risposta di Errore (HTTP 400 Bad Request):** Indica che c'è stato un problema durante la creazione dell'intento.
    ```json
    {
        "error": "An error occurred with Stripe."
    }
    ```

### 2. Salvare un Ordine nel Database

Questo endpoint dovrebbe essere chiamato dal frontend **solo dopo** che il pagamento è stato confermato con successo tramite Stripe.js.

-   **URL:** `http://localhost:8080/api/orders/create`
-   **Metodo HTTP:** `POST`
-   **Corpo della Richiesta (Payload JSON):** Tutti i dati dell'ordine (indirizzo, prodotti, ecc.).

### 3. Iscriversi alla Newsletter

-   **URL:** `http://localhost:8080/api/newsletter/subscribe`
-   **Metodo HTTP:** `POST`
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
