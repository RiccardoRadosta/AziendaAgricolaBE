# Backend E-commerce con Spring Boot e Stripe

## Riepilogo dell'Applicazione

Questa è un'applicazione backend basata su Java e il framework Spring Boot, progettata per servire un e-commerce. Integra il sistema di pagamento **Stripe** per processare gli addebiti in modo sicuro.

### Funzionalità Principali

1.  **Gestione Pagamenti e Ordini:** Riceve i dettagli di un ordine e un token di pagamento sicuro da Stripe, processa l'addebito e, solo in caso di successo, salva l'ordine su un database. Gestisce i flussi di autenticazione sicura (3D Secure).
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

### 1. Processare un Pagamento e Creare un Ordine

-   **URL:** `http://localhost:8080/api/orders/charge`
-   **Metodo HTTP:** `POST`
-   **Corpo della Richiesta (Payload JSON):**
    Il payload deve contenere i dettagli del cliente, dell'ordine e il `paymentToken` generato da Stripe.js nel frontend.
    ```json
    {
        "fullName": "...",
        "email": "...",
        "phone": "...",
        "address": "...",
        "city": "...",
        "province": "...",
        "postalCode": "...",
        "country": "...",
        "newsletterSubscribed": true,
        "orderNotes": "...",
        "items": "[{\"name\":\"Prodotto 1\",\"quantity\":2}]",
        "subtotal": 50.00,
        "paymentToken": "pm_xxxxxxxxxxxx"
    }
    ```

#### Risposte dell'Endpoint `/charge`

-   **Risposta di Successo Immediato (HTTP 200 OK):** Indica che il pagamento è stato processato con successo senza ulteriori autenticazioni. L'ordine è stato salvato.
    ```json
    {
        "status": "succeeded"
    }
    ```

-   **Risposta per Autenticazione Aggiuntiva (HTTP 200 OK):** Indica che il pagamento richiede un'azione da parte dell'utente (es. 3D Secure).
    ```json
    {
        "status": "requires_action",
        "clientSecret": "pi_xxxxxxxxxxxx_secret_xxxxxxxx"
    }
    ```
    Il frontend deve usare il `clientSecret` con Stripe.js per completare l'autenticazione.

-   **Risposta di Errore (HTTP 400 Bad Request):** Indica che il pagamento è fallito (es. carta rifiutata). L'ordine non è stato salvato.
    ```json
    {
        "error": "Your card was declined."
    }
    ```

### 2. Iscriversi alla Newsletter

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
