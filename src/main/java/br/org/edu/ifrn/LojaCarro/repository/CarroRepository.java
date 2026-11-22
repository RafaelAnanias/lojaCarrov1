package br.org.edu.ifrn.LojaCarro.repository;

import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.model.SituacaoVeiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarroRepository extends JpaRepository<Carro, Long> {

    Optional<Carro> findFirstByModelo(String modelo);

    List<Carro> findBySituacaoNot(SituacaoVeiculo situacao);

    @Query("SELECT c FROM Carro c WHERE c.situacao <> 'INATIVO' " +
            "AND (:marca IS NULL OR LOWER(c.marca) LIKE LOWER(CONCAT('%', :marca, '%'))) " +
            "AND (:modelo IS NULL OR LOWER(c.modelo) LIKE LOWER(CONCAT('%', :modelo, '%'))) " +
            "AND (:precoMin IS NULL OR c.preco >= :precoMin) " +
            "AND (:precoMax IS NULL OR c.preco <= :precoMax) " +
            "AND (:anoMin IS NULL OR c.anoModelo >= :anoMin) " +
            "AND (:anoMax IS NULL OR c.anoModelo <= :anoMax)")
    List<Carro> filtrarCarros(
            @Param("marca") String marca,
            @Param("modelo") String modelo,
            @Param("precoMin") Double precoMin,
            @Param("precoMax") Double precoMax,
            @Param("anoMin") Integer anoMin,
            @Param("anoMax") Integer anoMax
    );
}