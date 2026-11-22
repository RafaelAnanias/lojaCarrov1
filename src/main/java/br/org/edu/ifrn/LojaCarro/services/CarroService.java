package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.CarroException;
import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.model.SituacaoVeiculo;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class CarroService {

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private ImagemService imagemService;

    public Carro save(Carro c, MultipartFile foto) {
        validarCampos(c);
        String nomeFoto = imagemService.salvarImagem(foto);
        c.setFotoPrincipal(nomeFoto);
        if (c.getSituacao() == null) {
            c.setSituacao(SituacaoVeiculo.DISPONIVEL);
        }
        return carroRepository.save(c);
    }

    public Carro update(Long id, Carro dadosNovos, MultipartFile novaFoto) {
        Carro existente = carroRepository.findById(id)
                .orElseThrow(() -> new CarroException("Carro com ID " + id + " não encontrado."));

        validarCampos(dadosNovos);

        existente.setMarca(dadosNovos.getMarca());
        existente.setModelo(dadosNovos.getModelo());
        existente.setAnoFabricacao(dadosNovos.getAnoFabricacao());
        existente.setAnoModelo(dadosNovos.getAnoModelo());
        existente.setCor(dadosNovos.getCor());
        existente.setQuilometragem(dadosNovos.getQuilometragem());
        existente.setTipoCombustivel(dadosNovos.getTipoCombustivel());
        existente.setTipoTransmissao(dadosNovos.getTipoTransmissao());
        existente.setPreco(dadosNovos.getPreco());
        existente.setDescricao(dadosNovos.getDescricao());

        if (dadosNovos.getSituacao() != null) {
            existente.setSituacao(dadosNovos.getSituacao());
        }

        if (novaFoto != null && !novaFoto.isEmpty()) {
            String nomeFoto = imagemService.salvarImagem(novaFoto);
            existente.setFotoPrincipal(nomeFoto);
        }

        return carroRepository.save(existente);
    }


    public Carro desativar(Long id) {
        return alterarSituacao(id, SituacaoVeiculo.INATIVO);
    }

    public Carro alterarSituacao(Long id, SituacaoVeiculo novaSituacao) {
        Carro carro = carroRepository.findById(id)
                .orElseThrow(() -> new CarroException("Carro não encontrado para o ID: " + id));
        carro.setSituacao(novaSituacao);
        return carroRepository.save(carro);
    }

    public Optional<Carro> findById(Long id) {
        if (id <= 0) {
            throw new CarroException("O ID do carro deve ser positivo.");
        }
        return carroRepository.findById(id);
    }

    public List<Carro> findAllAtivos() {
        return carroRepository.findBySituacaoNot(SituacaoVeiculo.INATIVO);
    }

    public List<Carro> filtrar(String marca, String modelo, Double precoMin, Double precoMax, Integer anoMin, Integer anoMax) {
        String marcaFiltro = (marca != null && !marca.isBlank()) ? marca.trim() : null;
        String modeloFiltro = (modelo != null && !modelo.isBlank()) ? modelo.trim() : null;
        return carroRepository.filtrarCarros(marcaFiltro, modeloFiltro, precoMin, precoMax, anoMin, anoMax);
    }

    private void validarCampos(Carro c) {
        if (c.getMarca() == null || c.getMarca().isBlank()) throw new CarroException("A marca é obrigatória.");
        if (c.getModelo() == null || c.getModelo().isBlank()) throw new CarroException("O modelo é obrigatório.");
        if (c.getAnoFabricacao() == null || c.getAnoFabricacao() < 1900) throw new CarroException("Ano de fabricação inválido.");
        if (c.getAnoModelo() == null || c.getAnoModelo() < 1900) throw new CarroException("Ano do modelo inválido.");
        if (c.getCor() == null || c.getCor().isBlank()) throw new CarroException("A cor é obrigatória.");
        if (c.getQuilometragem() == null || c.getQuilometragem() < 0) throw new CarroException("Quilometragem inválida.");
        if (c.getTipoCombustivel() == null) throw new CarroException("O tipo de combustível é obrigatório.");
        if (c.getTipoTransmissao() == null) throw new CarroException("O tipo de transmissão é obrigatório.");
        if (c.getPreco() == null || c.getPreco() < 0) throw new CarroException("O preço não pode ser negativo.");
    }
}