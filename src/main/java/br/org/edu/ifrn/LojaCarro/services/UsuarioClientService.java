package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UsuarioClientService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioClientService.class);

    @Autowired
    private WebClient usuarioWebClient;

    public Usuario buscarPorId(Long id) {
        log.info("Consultando usuário ID {} na API de usuários", id);

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String authHeader = (attrs != null) ? attrs.getRequest().getHeader("Authorization") : null;

        return usuarioWebClient.get()
                .uri("/usuario/{id}", id)
                .headers(headers -> {
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(Usuario.class)
                .onErrorResume(e -> {
                    log.error("Erro ao consultar usuário ID {}: {}", id, e.getMessage());
                    return Mono.error(new RuntimeException("Não foi possível buscar o usuário: " + e.getMessage()));
                })
                .block();
    }
}