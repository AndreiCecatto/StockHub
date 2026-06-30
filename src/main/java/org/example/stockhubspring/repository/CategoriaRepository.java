package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    List<Categoria> findByUsuarioCodigoOrderByNomeAsc(Integer usuarioCodigo);

    List<Categoria> findByUsuarioCodigoAndNomeContainingIgnoreCaseOrderByNomeAsc(Integer usuarioCodigo, String nome);
}
