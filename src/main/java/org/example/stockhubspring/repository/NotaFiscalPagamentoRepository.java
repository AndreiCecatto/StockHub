package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.NotaFiscalPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** O acesso sempre passa antes pela nota da loja (NotaFiscalRepository.findByIdAndUsuarioCodigo). */
public interface NotaFiscalPagamentoRepository extends JpaRepository<NotaFiscalPagamento, Integer> {

    List<NotaFiscalPagamento> findByNotaFiscalIdOrderByOrdemAsc(Integer notaFiscalId);
}
