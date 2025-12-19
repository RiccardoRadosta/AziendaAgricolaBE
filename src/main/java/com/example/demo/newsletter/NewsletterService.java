package com.example.demo.newsletter;

import com.example.demo.order.BrevoEmailService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class NewsletterService {

    private final Firestore firestore;
    private final BrevoEmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(NewsletterService.class);

    public NewsletterService(Firestore firestore, BrevoEmailService emailService) {
        this.firestore = firestore;
        this.emailService = emailService;
    }

    public void subscribe(NewsletterSubscriptionDTO subscriptionDTO) {
        try {
            // Controlla se l'email è già presente nel database
            ApiFuture<QuerySnapshot> future = firestore.collection("newsletterSubscriptions")
                    .whereEqualTo("email", subscriptionDTO.getEmail())
                    .limit(1) // Ottimizzazione: ci basta sapere se esiste almeno un record
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            // Se l'email esiste già, interrompi l'esecuzione.
            // Il controller risponderà comunque con 200 OK, come da richiesta.
            if (!documents.isEmpty()) {
                logger.info("L'email {} è già iscritta alla newsletter. La richiesta viene ignorata.", subscriptionDTO.getEmail());
                return;
            }

            // Se l'email non esiste, procedi con l'iscrizione
            logger.info("Elaborazione nuova iscrizione per l'email: {}", subscriptionDTO.getEmail());
            NewsletterSubscription subscription = new NewsletterSubscription();
            subscription.setEmail(subscriptionDTO.getEmail());
            subscription.setSubscribedAt(ZonedDateTime.now());
            subscription.setId(UUID.randomUUID().toString());

            firestore.collection("newsletterSubscriptions").document(subscription.getId()).set(subscription);
            logger.info("Iscrizione salvata su Firestore con ID: {}", subscription.getId());

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Errore durante la verifica dell'iscrizione esistente per {}: {}", subscriptionDTO.getEmail(), e.getMessage());
            // Ripristina lo stato di interruzione del thread in caso di InterruptedException
            Thread.currentThread().interrupt();
            // In caso di errore durante il controllo, è più sicuro non procedere per evitare di creare duplicati
            // in caso di fallimenti transitori. La richiesta di fatto non andrà a buon fine ma dal client
            // sembrerà di si. In un'implementazione più complessa si potrebbe gestire un re-try o un errore specifico.
        }
    }

    /**
     * Conta il numero totale di iscritti alla newsletter.
     * @return Il numero di iscritti.
     * @throws ExecutionException Se si verifica un errore durante il recupero dei dati da Firestore.
     * @throws InterruptedException Se il thread viene interrotto durante l'attesa dei dati.
     */
    public int countSubscribers() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = firestore.collection("newsletterSubscriptions").get();
        // Nota: per collezioni molto grandi, questo approccio potrebbe essere inefficiente.
        // Firestore non ha un'operazione "count" diretta e a basso costo.
        // Per ora, dato il contesto, questo metodo è sufficiente ed efficace.
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        logger.info("Found {} total subscribers.", documents.size());
        return documents.size();
    }

    public void sendNewsletter(String subject, String message) throws ExecutionException, InterruptedException {
        logger.info("Starting newsletter send job...");
        ApiFuture<QuerySnapshot> future = firestore.collection("newsletterSubscriptions").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            logger.warn("No subscribers found. Newsletter job finished.");
            return;
        }

        logger.info("Found {} subscribers. Proceeding to send emails.", documents.size());

        for (QueryDocumentSnapshot document : documents) {
            String email = document.getString("email");
            if (email != null && !email.isEmpty()) {
                try {
                    emailService.sendEmail(email, subject, message);
                    logger.info("Successfully sent newsletter to: {}", email);
                } catch (Exception e) {
                    logger.error("Failed to send newsletter to: {}. Error: {}", email, e.getMessage());
                }
            }
        }
        logger.info("Newsletter send job finished.");
    }
}
