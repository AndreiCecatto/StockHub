package org.example.stockhubspring.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.stockhubspring.repository.MembroLojaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Corta a sessão de quem foi desativado (RN-65): a cada pedido autenticado confere se o membro continua
 * ativo. Se não estiver, a sessão é encerrada e o pedido segue como anônimo (vai para o login).
 * Sessão sem membroId (conta antiga, sem membro associado) não é conferida.
 */
public class MembroAtivoFilter extends OncePerRequestFilter {

    private final MembroLojaRepository membroRepository;

    public MembroAtivoFilter(MembroLojaRepository membroRepository) {
        this.membroRepository = membroRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioPrincipal principal
                && principal.getMembroId() != null
                && !membroRepository.existsByIdAndAtivoTrue(principal.getMembroId())) {
            SecurityContextHolder.clearContext();
            HttpSession sessao = request.getSession(false);
            if (sessao != null) {
                sessao.invalidate();
            }
        }
        chain.doFilter(request, response);
    }
}
