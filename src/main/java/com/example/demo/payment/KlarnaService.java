package com.example.demo.payment;

import com.example.demo.order.OrderDTO;
import com.example.demo.order.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class KlarnaService {

    private static final Logger logger = LoggerFactory.getLogger(KlarnaService.class);

    private final OrderService orderService;
    private final Firestore firestore;
    private final ObjectMapper objectMapper;

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    public KlarnaService(OrderService orderService, Firestore firestore, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.firestore = firestore;
        this.objectMapper = objectMapper;
    }

    public String createCheckoutSession(OrderDTO orderDTO) throws StripeException, IOException, ExecutionException, InterruptedException {
        Stripe.apiKey = stripeApiKey;

        double serverTotal = orderService.calculateOrderTotal(orderDTO);
        long amountInCents = (long) (serverTotal * 100);

        String tempId = UUID.randomUUID().toString();
        Map<String, Object> orderData = objectMapper.convertValue(orderDTO, Map.class);
        orderData.put("createdAt", System.currentTimeMillis());
        
        firestore.collection("checkout_sessions").document(tempId).set(orderData).get();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.KLARNA)
                .setSuccessUrl(orderDTO.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(orderDTO.getCancelUrl())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Ordine Azienda Agricola")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("tempOrderId", tempId)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public void verifyAndCreateOrder(String sessionId) throws StripeException, ExecutionException, InterruptedException, IOException {
        Stripe.apiKey = stripeApiKey;

        Session session = Session.retrieve(sessionId);

        if (!"paid".equals(session.getPaymentStatus())) {
            throw new RuntimeException("Payment not completed. Status: " + session.getPaymentStatus());
        }

        String tempOrderId = session.getMetadata().get("tempOrderId");
        if (tempOrderId == null) {
            throw new RuntimeException("No tempOrderId found in session metadata");
        }

        DocumentReference tempOrderRef = firestore.collection("checkout_sessions").document(tempOrderId);

        Map<String, Object> orderData = firestore.runTransaction(new Transaction.Function<Map<String, Object>>() {
            @Override
            public Map<String, Object> updateCallback(Transaction transaction) throws Exception {
                DocumentSnapshot snapshot = transaction.get(tempOrderRef).get();
                
                if (!snapshot.exists()) {
                    return null;
                }
                
                Map<String, Object> data = snapshot.getData();
                transaction.delete(tempOrderRef);
                
                return data;
            }
        }).get();

        String paymentIntentId = session.getPaymentIntent();

        if (orderData == null) {
            if (paymentIntentId != null) {
                QuerySnapshot existingOrders = firestore.collection("orders")
                        .whereEqualTo("paymentToken", paymentIntentId)
                        .limit(1)
                        .get().get();
                if (!existingOrders.isEmpty()) {
                    logger.info("Order already exists (race condition handled). Skipping creation for session: {}", sessionId);
                    return;
                }
            }
            logger.warn("Temp order {} not found during transaction. Assuming already processed.", tempOrderId);
            return;
        }

        OrderDTO orderDTO = objectMapper.convertValue(orderData, OrderDTO.class);
        
        orderDTO.setPaymentMethod("klarna");
        orderDTO.setPaymentToken(paymentIntentId);

        // --- RECUPERO FEE STRIPE (KLARNA) ---
        if (paymentIntentId != null) {
            try {
                List<String> expandList = new ArrayList<>();
                expandList.add("latest_charge.balance_transaction");

                Map<String, Object> params = new HashMap<>();
                params.put("expand", expandList);

                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId, params, null);
                
                Charge latestCharge = (Charge) paymentIntent.getLatestChargeObject();
                if (latestCharge != null) {
                    BalanceTransaction balanceTransaction = latestCharge.getBalanceTransactionObject();
                    if (balanceTransaction != null) {
                        long feeCents = balanceTransaction.getFee();
                        long netCents = balanceTransaction.getNet();
                        
                        double feeEuro = feeCents / 100.0;
                        double netEuro = netCents / 100.0;
                        
                        orderDTO.setPaymentFee(feeEuro);
                        orderDTO.setNetRevenue(netEuro);
                        
                        logger.info("Klarna/Stripe Fee: {} EUR, Net: {} EUR", feeEuro, netEuro);
                    }
                }
            } catch (Exception e) {
                logger.error("Error retrieving Klarna fees from Stripe: {}", e.getMessage());
            }
        }
        // ------------------------------------

        orderService.createOrder(orderDTO);
        logger.info("Order created successfully for session: {}", sessionId);
    }
}
