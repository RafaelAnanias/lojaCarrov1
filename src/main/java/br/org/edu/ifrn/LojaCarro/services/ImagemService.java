package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.CarroException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class ImagemService {

    private static final long TAMANHO_MAXIMO = 5 * 1024 * 1024; // 5 MB
    private static final List<String> TIPOS_PERMITIDOS = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private final Path pastaImg = Paths.get("IMG");

    public String salvarImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new CarroException("A fotografia principal do veículo é obrigatória.");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new CarroException("A imagem excede o tamanho máximo permitido de 5 MB.");
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new CarroException("Formato inválido. Aceitos apenas: JPG, JPEG, PNG ou WebP.");
        }

        try {
            if (!Files.exists(pastaImg)) {
                Files.createDirectories(pastaImg);
            }

            String extensao = extrairExtensao(arquivo.getOriginalFilename());
            String nomeUnico = UUID.randomUUID() + extensao;
            Path destino = pastaImg.resolve(nomeUnico);

            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return nomeUnico;
        } catch (IOException e) {
            throw new CarroException("Erro ao armazenar a imagem na pasta IMG: " + e.getMessage());
        }
    }

    private String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            return nomeOriginal.substring(nomeOriginal.lastIndexOf(".")).toLowerCase();
        }
        return ".jpg";
    }
}