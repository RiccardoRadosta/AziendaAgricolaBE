package com.example.demo.blog;

import lombok.Data;
import java.util.List;

@Data
public class ArticleDTO {
    private String title;
    private String content;
    private List<String> imageUrls;
    private List<String> relatedProductIds;
    private String status; // "DRAFT", "PUBLISHED"
}
