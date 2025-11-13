# Backend E-commerce con Spring Boot e Stripe

## Riepilogo dell'Applicazione

Questa è un'applicazione backend basata su Java e il framework Spring Boot, progettata per servire un e-commerce. Integra il sistema di pagamento **Stripe** per processare gli addebiti in modo sicuro.

### Funzionalità Principali

1.  **Gestione Pagamenti e Ordini:** Riceve i dettagli di un ordine e un token di pagamento sicuro da Stripe, processa l'addebito e, solo in caso di successo, salva l'ordine su un database.
2.  **Iscrizione Newsletter:** Salva l'email di un utente nel database per future comunicazioni.

---

## Prerequisiti

Prima di avviare l'applicazione, è necessario configurare la propria chiave segreta di Stripe.

1.  Crea un account di test su [Stripe](https://dashboard.stripe.com/register).
2.  Trova la tua **chiave segreta** (Secret Key) nella sezione "Sviluppatori" > "Chiavi API".
3.  Apri il file `src/main/resources/application.properties`.
4.  Aggiungi o modifica la seguente riga, incollando la tua chiave:
    ```properties
    stripe.secret.key=sk_test_xxxxxxxxxxxxxxxxxxxx
    ```

---

## Guida all'Avvio e al Test

Per testare l'applicazione, è necessario avviare il server.

```bash
./mvnw spring-boot:run
```

Questo comando avvierà l'applicazione sulla porta `8080`.

---

## Documentazione API (Endpoint per il Frontend)

Una volta che il server è in esecuzione, i seguenti endpoint saranno disponibili.

### 1. Processare un Pagamento e Creare un Ordine

Questo endpoint è il cuore del sistema di checkout. Utilizza Stripe per un pagamento sicuro.

-   **URL:** `http://localhost:8080/api/orders/charge`
-   **Metodo HTTP:** `POST`
-   **Headers:** `Content-Type: application/json`
-   **Logica:** Il frontend deve prima creare un `PaymentMethod` usando Stripe.js. L'ID di questo `PaymentMethod` (`pm_...`) va inviato nel campo `paymentToken`.

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
        "orderNotes": "Consegnare al portiere.",
        "items": "[{\"productName\": \"T-Shirt Nera\"}]",
        "subtotal": 89.99,
        "paymentToken": "pm_xxxxxxxxxxxxxx" 
    }
    ```

-   **Risposta di Successo (HTTP 200 OK):** Indica che il pagamento è riuscito e l'ordine è stato salvato.

    ```json
    {
        "status": "Payment successful and order created"
    }
    ```

-   **Risposta di Errore (HTTP 400 Bad Request):** Indica che il pagamento è fallito (es. carta rifiutata). L'ordine non è stato salvato.

    ```json
    {
        "status": "Your card was declined."
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
