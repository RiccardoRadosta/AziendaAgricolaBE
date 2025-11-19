package com.example.demo.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public ProductController(ProductService productService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Product> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart("images") List<MultipartFile> images) {
        try {
            ProductDTO productDTO = objectMapper.readValue(productJson, ProductDTO.class);
            Product createdProduct = productService.createProduct(productDTO, images);
            return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace(); // È buona norma loggare l'eccezione
            // Potresti voler gestire diversi tipi di eccezioni con risposte diverse
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
