package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.Modulo;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.EnumMap;
import java.util.Map;

@ControllerAdvice
public class GlobalModelAdvice {

    private final UsuarioAtualService usuarioAtualService;
    private final UsuarioRepository usuarioRepository;

    public GlobalModelAdvice(UsuarioAtualService usuarioAtualService, UsuarioRepository usuarioRepository) {
        this.usuarioAtualService = usuarioAtualService;
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("usuarioLogado")
    public Usuario usuarioLogado() {
        return usuarioAtualService.getUsuarioAtualOuNull();
    }

    /**
     * Tipo da loja logada (ESTOQUE se não houver sessão). Lido do banco a cada requisição, e não da sessão,
     * para que a troca de tipo valha na hora (RN-43).
     */
    @ModelAttribute("tipoLoja")
    public TipoLoja tipoLoja() {
        Usuario usuario = usuarioAtualService.getUsuarioAtualOuNull();
        if (usuario == null || usuario.getCodigo() == null) {
            return TipoLoja.ESTOQUE;
        }
        return usuarioRepository.findTipoLojaByCodigo(usuario.getCodigo()).orElse(TipoLoja.ESTOQUE);
    }

    /** Papel de quem está logado (para a tela esconder o que o papel não pode usar; o servidor confere de novo). */
    @ModelAttribute("papelLogado")
    public PapelMembro papelLogado() {
        Usuario usuario = usuarioAtualService.getUsuarioAtualOuNull();
        return usuario == null ? null : usuario.getPapel();
    }

    /** Dono ou gerente: pode excluir, mudar o tipo da loja e cuidar da equipe (RN-62). */
    @ModelAttribute("podeGerenciar")
    public boolean podeGerenciar() {
        PapelMembro papel = papelLogado();
        return papel != null && papel.gerenciaLoja();
    }

    /** Módulos ligados para os JSPs: {@code ${modulos['VENDAS']}} (ver tags/modulo.tag). */
    @ModelAttribute("modulos")
    public Map<String, Boolean> modulos() {
        TipoLoja tipo = tipoLoja();
        Map<Modulo, Boolean> mapa = new EnumMap<>(Modulo.class);
        for (Modulo m : Modulo.values()) {
            mapa.put(m, tipo.temModulo(m));
        }
        Map<String, Boolean> porNome = new java.util.LinkedHashMap<>();
        mapa.forEach((m, ligado) -> porNome.put(m.name(), ligado));
        return porNome;
    }
}
