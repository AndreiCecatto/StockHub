package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.NotaFiscalParcela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** O acesso sempre passa antes pela nota da loja (NotaFiscalRepository.findByIdAndUsuarioCodigo). */
public interface NotaFiscalParcelaRepository extends JpaRepository<NotaFiscalParcela, Integer> {

    List<NotaFiscalParcela> findByNotaFiscalIdOrderByNumeroAsc(Integer notaFiscalId);
}
