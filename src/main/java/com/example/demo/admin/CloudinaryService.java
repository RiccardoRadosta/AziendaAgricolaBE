package com.example.demo.admin;

import com.cloudinary.Cloudinary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Regex per estrarre la parte dell'URL dopo /upload/ e prima dell'estensione del file.
    // Esempio: .../upload/v123456/folder/image.jpg -> v123456/folder/image
    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile(".*/upload/(?:v\\d+/)?(.*?)(?:\\.\\w+)?$");

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Elimina un'immagine da Cloudinary.
     * Il metodo non restituisce nulla. Il successo è indicato dall'assenza di eccezioni.
     * @param imageUrl L'URL completo dell'immagine da eliminare.
     * @throws IOException Se c'è un errore di comunicazione con Cloudinary.
     * @throws IllegalArgumentException Se l'URL non è valido e non è possibile estrarre un Public ID.
     */
    public void deleteImage(String imageUrl) throws IOException {
        String publicId = extractPublicIdFromUrl(imageUrl);

        // Chiama il metodo destroy. Non abbiamo bisogno del risultato, quindi non lo assegniamo.
        // L'opzione "invalidate" true è utile se si usa una CDN per assicurarsi che venga rimossa dalla cache.
        cloudinary.uploader().destroy(publicId, Map.of("invalidate", true));
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        Matcher matcher = PUBLIC_ID_PATTERN.matcher(imageUrl);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        // Se l'URL non corrisponde (improbabile), lancia un'eccezione.
        throw new IllegalArgumentException("Invalid Cloudinary URL, cannot extract public ID: " + imageUrl);
    }
}
