package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.CarroException;
import br.org.edu.ifrn.LojaCarro.dto.CarroMensagemDTO;
import br.org.edu.ifrn.LojaCarro.mensageria.Enviar;
import br.org.edu.ifrn.LojaCarro.model.*;
import br.org.edu.ifrn.LojaCarro.services.CarroService;
import br.org.edu.ifrn.LojaCarro.services.LogAuditoriaService;
import br.org.edu.ifrn.LojaCarro.services.UsuarioClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/carro")
@CrossOrigin(origins = "*")
public class CarroController {

    private static final String FILA_RABBITMQ = "claudemyr_rafael";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CarroService carroService;

    @Autowired
    private UsuarioClientService usuarioClientService;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    @PostMapping(value = "/salvar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Carro> salvarCarro(
            @RequestParam Long usuarioId,
            @RequestPart("carro") String carroJson,
            @RequestPart("foto") MultipartFile foto) {

        Usuario usuario = usuarioClientService.buscarPorId(usuarioId);
        if (usuario.getCargo() != Cargo.GERENTE && usuario.getCargo() != Cargo.ADMIN) {
            logAuditoriaService.registrar(usuario, Acao.CADASTRO, "CARRO", null, "FALHA");
            throw new CarroException("Acesso negado: Apenas Gerente ou Admin pode cadastrar veículos.");
        }

        try {
            Carro c = objectMapper.readValue(carroJson, Carro.class);
            Carro savedCarro = carroService.save(c, foto);

            logAuditoriaService.registrar(usuario, Acao.CADASTRO, "CARRO", savedCarro.getId(), "SUCESSO");
            enviarParaFila(savedCarro);

            return ResponseEntity.ok(savedCarro);
        } catch (CarroException ex) {
            logAuditoriaService.registrar(usuario, Acao.CADASTRO, "CARRO", null, "FALHA");
            throw ex;
        } catch (Exception ex) {
            logAuditoriaService.registrar(usuario, Acao.CADASTRO, "CARRO", null, "FALHA");
            throw new CarroException("Erro ao processar dados do veículo: " + ex.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Carro> atualizarCarro(
            @PathVariable Long id,
            @RequestParam Long usuarioId,
            @RequestPart("carro") String carroJson,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {

        Usuario usuario = usuarioClientService.buscarPorId(usuarioId);
        try {
            Carro c = objectMapper.readValue(carroJson, Carro.class);
            Carro updatedCarro = carroService.update(id, c, foto);

            logAuditoriaService.registrar(usuario, Acao.ATUALIZACAO, "CARRO", updatedCarro.getId(), "SUCESSO");
            enviarParaFila(updatedCarro);

            return ResponseEntity.ok(updatedCarro);
        } catch (CarroException ex) {
            logAuditoriaService.registrar(usuario, Acao.ATUALIZACAO, "CARRO", id, "FALHA");
            throw ex;
        } catch (Exception ex) {
            logAuditoriaService.registrar(usuario, Acao.ATUALIZACAO, "CARRO", id, "FALHA");
            throw new CarroException("Erro ao atualizar veículo: " + ex.getMessage());
        }
    }

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<Carro> alterarSituacao(
            @PathVariable Long id,
            @RequestParam Long usuarioId,
            @RequestParam SituacaoVeiculo situacao) {

        Usuario usuario = usuarioClientService.buscarPorId(usuarioId);
        try {
            Carro carro = carroService.alterarSituacao(id, situacao);
            Acao acao = (situacao == SituacaoVeiculo.INATIVO) ? Acao.EXCLUSAO : Acao.ATUALIZACAO;
            logAuditoriaService.registrar(usuario, acao, "CARRO", id, "SUCESSO");
            enviarParaFila(carro);
            return ResponseEntity.ok(carro);
        } catch (CarroException ex) {
            logAuditoriaService.registrar(usuario, Acao.ATUALIZACAO, "CARRO", id, "FALHA");
            throw ex;
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCarro(@PathVariable Long id, @RequestParam Long usuarioId) {
        Usuario usuario = usuarioClientService.buscarPorId(usuarioId);
        try {
            Carro carroDesativado = carroService.desativar(id);
            logAuditoriaService.registrar(usuario, Acao.EXCLUSAO, "CARRO", id, "SUCESSO");
            enviarParaFila(carroDesativado);
            return ResponseEntity.noContent().build();
        } catch (CarroException ex) {
            logAuditoriaService.registrar(usuario, Acao.EXCLUSAO, "CARRO", id, "FALHA");
            throw ex;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Carro> pesquisarCarroPorId(@PathVariable Long id) {
        return carroService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/listarCarros")
    public ResponseEntity<List<Carro>> pesquisarTodosCarros() {
        return ResponseEntity.ok(carroService.findAllAtivos());
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<Carro>> filtrarCarros(
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) Double precoMin,
            @RequestParam(required = false) Double precoMax,
            @RequestParam(required = false) Integer anoMin,
            @RequestParam(required = false) Integer anoMax) {

        return ResponseEntity.ok(carroService.filtrar(marca, modelo, precoMin, precoMax, anoMin, anoMax));
    }

    private void enviarParaFila(Carro carro) {
        CarroMensagemDTO msg = new CarroMensagemDTO(
                carro.getId(),
                carro.getMarca() + " " + carro.getModelo(),
                carro.getAnoModelo(),
                carro.getPreco(),
                LocalDateTime.now().toString()
        );
        Enviar.enviarMensagem(msg, FILA_RABBITMQ);
    }
}