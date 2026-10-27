package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.OsServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OsServicoRepository extends JpaRepository<OsServico, Integer> {

    List<OsServico> findByOsIdOrderByIdAsc(Integer osId);
}
