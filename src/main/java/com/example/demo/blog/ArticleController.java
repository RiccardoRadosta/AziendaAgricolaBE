package com.example.demo.blog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    // --- ENDPOINT PUBBLICI ---

    @GetMapping("/blog")
    public ResponseEntity<List<Article>> getPublishedArticles() {
        try {
            List<Article> articles = articleService.getPublishedArticles();
            return ResponseEntity.ok(articles);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/blog/{slug}")
    public ResponseEntity<Article> getArticleBySlug(@PathVariable String slug) {
        try {
            Article article = articleService.getArticleBySlug(slug);
            if (article != null) {
                return ResponseEntity.ok(article);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- ENDPOINT ADMIN ---

    @GetMapping("/admin/blog")
    public ResponseEntity<List<Article>> getAllArticlesForAdmin() {
        try {
            List<Article> articles = articleService.getAllArticlesForAdmin();
            return ResponseEntity.ok(articles);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/admin/blog/{id}")
    public ResponseEntity<Article> getArticleByIdForAdmin(@PathVariable String id) {
        try {
            Article article = articleService.getArticleById(id);
            if (article != null) {
                return ResponseEntity.ok(article);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/admin/blog")
    public ResponseEntity<?> createArticle(@RequestBody ArticleDTO articleDTO) {
        try {
            String articleId = articleService.createArticle(articleDTO);
            return new ResponseEntity<>(Map.of("id", articleId, "message", "Article created successfully"), HttpStatus.CREATED);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating article: " + e.getMessage());
        }
    }

    @PutMapping("/admin/blog/{id}")
    public ResponseEntity<?> updateArticle(@PathVariable String id, @RequestBody ArticleDTO articleDTO) {
        try {
            articleService.updateArticle(id, articleDTO);
            return ResponseEntity.ok(Map.of("message", "Article updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating article: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/blog/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable String id) {
        try {
            articleService.deleteArticle(id);
            return ResponseEntity.ok(Map.of("message", "Article deleted successfully"));
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting article: " + e.getMessage());
        }
    }
}
