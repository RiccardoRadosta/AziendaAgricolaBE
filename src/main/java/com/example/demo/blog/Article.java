package com.example.demo.blog;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

import java.util.List;

@Data
public class Article {
    @DocumentId
    private String id;
    
    private String title;
    private String slug;
    private String content;
    private List<String> imageUrls;
    private List<String> relatedProductIds;
    private String status; // "DRAFT", "PUBLISHED"
    
    private Timestamp createdAt;
    private Timestamp publishedAt;
}
