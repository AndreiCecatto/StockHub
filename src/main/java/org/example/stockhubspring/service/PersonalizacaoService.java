package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.BlocoTela;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.PreferenciaBloco;
import org.example.stockhubspring.model.TelaPersonalizavel;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.repository.PreferenciaBlocoRepository;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Personalização dos blocos das telas pelo dono ou gerente (AD-19, RN-109 a RN-114). */
@Service
public class PersonalizacaoService {

    private final PreferenciaBlocoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public PersonalizacaoService(PreferenciaBlocoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Blocos aplicáveis ao tipo da loja, com o estado atual; sem linha gravada, visível (RN-109). */
    @Transactional(readOnly = true)
    public Map<String, Boolean> blocosVisiveis(Integer lojaCodigo, TelaPersonalizavel tela) {
        Map<String, Boolean> mapa = new LinkedHashMap<>();
        for (BlocoTela bloco : BlocoTela.daTela(tela, tipoDaLoja(lojaCodigo))) {
            mapa.put(bloco.name(), true);
        }
        for (PreferenciaBloco p : repository.findByUsuarioCodigoAndTela(lojaCodigo, tela.name())) {
            // Código desconhecido ou de módulo que a loja não tem é ignorado (RN-111)
            if (mapa.containsKey(p.getBloco())) {
                mapa.put(p.getBloco(), p.isVisivel());
            }
        }
        return mapa;
    }

    /** Blocos aplicáveis à loja, na ordem da tela (para a tela de personalização). */
    @Transactional(readOnly = true)
    public List<BlocoTela> blocosAplicaveis(Integer lojaCodigo, TelaPersonalizavel tela) {
        return BlocoTela.daTela(tela, tipoDaLoja(lojaCodigo));
    }

    /** Grava uma linha por bloco aplicável: marcado = visível, o resto oculto. Nunca apaga (RN-113). */
    @Transactional
    public void salvar(Integer lojaCodigo, PapelMembro papel, Integer membroId, TelaPersonalizavel tela,
                       Set<String> marcados) {
        if (papel == null || !papel.podeGerenciarMembros()) {
            throw new RegraNegocioException("Só o dono ou um gerente personaliza a loja.");
        }
        if (tela == null) {
            throw new RegraNegocioException("Tela inválida para personalizar.");
        }
        List<BlocoTela> aplicaveis = BlocoTela.daTela(tela, tipoDaLoja(lojaCodigo));
        Set<BlocoTela> escolhidos = new java.util.HashSet<>();
        for (String codigo : marcados == null ? Set.<String>of() : marcados) {
            BlocoTela bloco = BlocoTela.de(codigo);
            if (bloco == null || !aplicaveis.contains(bloco)) {
                throw new RegraNegocioException("Bloco inválido para esta loja: escolha só os blocos da lista.");
            }
            escolhidos.add(bloco);
        }
        for (BlocoTela bloco : aplicaveis) {
            repository.gravar(lojaCodigo, tela.name(), bloco.name(), escolhidos.contains(bloco), membroId);
        }
    }

    private TipoLoja tipoDaLoja(Integer lojaCodigo) {
        return usuarioRepository.findTipoLojaByCodigo(lojaCodigo).orElse(TipoLoja.ESTOQUE);
    }
}
