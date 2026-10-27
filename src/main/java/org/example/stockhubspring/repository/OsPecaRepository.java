package org.example.stockhubspring.repository;

import org.example.stockhubspring.model.OsPeca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OsPecaRepository extends JpaRepository<OsPeca, Integer> {

    /** Peças da OS com o produto carregado, em ordem de produto e id (ordem estável das travas). */
    @Query("select p from OsPeca p join fetch p.produto where p.os.id = :osId order by p.produto.id, p.id")
    List<OsPeca> listarDaOs(@Param("osId") Integer osId);
}
