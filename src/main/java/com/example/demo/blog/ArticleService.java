package com.example.demo.blog;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final Firestore firestore;
    private final CollectionReference articlesCollection;

    public ArticleService(Firestore firestore) {
        this.firestore = firestore;
        this.articlesCollection = firestore.collection("articles");
    }

    // Pubblico: Solo articoli pubblicati, ordinati per data di pubblicazione decrescente
    public List<Article> getPublishedArticles() throws ExecutionException, InterruptedException {
        Query query = articlesCollection
                .whereEqualTo("status", "PUBLISHED")
                .orderBy("publishedAt", Query.Direction.DESCENDING);

        return query.get().get().toObjects(Article.class);
    }

    // Admin: Tutti gli articoli
    public List<Article> getAllArticlesForAdmin() throws ExecutionException, InterruptedException {
        return articlesCollection.orderBy("createdAt", Query.Direction.DESCENDING).get().get().toObjects(Article.class);
    }

    public Article getArticleBySlug(String slug) throws ExecutionException, InterruptedException {
        Query query = articlesCollection.whereEqualTo("slug", slug).limit(1);
        List<Article> articles = query.get().get().toObjects(Article.class);
        return articles.isEmpty() ? null : articles.get(0);
    }
    
    public Article getArticleById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = articlesCollection.document(id).get().get();
        return doc.exists() ? doc.toObject(Article.class) : null;
    }

    public String createArticle(ArticleDTO dto) throws ExecutionException, InterruptedException {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setImageUrls(dto.getImageUrls());
        article.setRelatedProductIds(dto.getRelatedProductIds());
        article.setStatus(dto.getStatus());
        
        article.setCreatedAt(Timestamp.now());
        if ("PUBLISHED".equals(dto.getStatus())) {
            article.setPublishedAt(Timestamp.now());
        }

        // Generazione Slug
        String slug = generateSlug(dto.getTitle());
        // Verifica unicità slug (semplificata: aggiunge suffisso se esiste già, o ci fidiamo della probabilità)
        // Per ora implementiamo una logica base, se serve unicità stretta bisognerebbe fare una query prima.
        article.setSlug(slug);

        ApiFuture<DocumentReference> future = articlesCollection.add(article);
        return future.get().getId();
    }

    public void updateArticle(String id, ArticleDTO dto) throws ExecutionException, InterruptedException {
        DocumentReference docRef = articlesCollection.document(id);
        DocumentSnapshot doc = docRef.get().get();
        
        if (!doc.exists()) {
            throw new IllegalArgumentException("Article not found");
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", dto.getTitle());
        updates.put("content", dto.getContent());
        updates.put("imageUrls", dto.getImageUrls());
        updates.put("relatedProductIds", dto.getRelatedProductIds());
        
        String oldStatus = doc.getString("status");
        updates.put("status", dto.getStatus());

        // Se passa da BOZZA a PUBBLICATO, aggiorna la data di pubblicazione
        if (!"PUBLISHED".equals(oldStatus) && "PUBLISHED".equals(dto.getStatus())) {
            updates.put("publishedAt", Timestamp.now());
        }
        
        // Opzionale: rigenerare lo slug se cambia il titolo? 
        // Di solito meglio di no per non rompere i link esistenti (SEO), quindi lo lasciamo invariato.

        docRef.update(updates).get();
    }

    public void deleteArticle(String id) throws ExecutionException, InterruptedException {
        articlesCollection.document(id).delete().get();
    }

    private String generateSlug(String title) {
        if (title == null) return "";
        String nowhitespace = title.trim().replaceAll("\\s+", "-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        return slug.toLowerCase().replaceAll("[^a-z0-9-]", "");
    }
}
