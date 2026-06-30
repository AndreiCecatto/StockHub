package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Integer> {
    List<MovimentacaoEstoque> findByUsuarioCodigoOrderByCriadoEmDesc(Integer usuarioCodigo);
}
