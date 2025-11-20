package com.example.demo.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody ProductDTO productDTO) {
        try {
            String productId = productService.createProduct(productDTO);
            return ResponseEntity.ok(productId);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body("Error creating product: " + e.getMessage());
        }
    }
}
