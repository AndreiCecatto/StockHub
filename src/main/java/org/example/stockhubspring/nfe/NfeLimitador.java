package org.example.stockhubspring.nfe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.LongSupplier;

/**
 * Proteção contra abuso das rotas de NF-e (contadores em memória, janela deslizante; zeram se a aplicação
 * reiniciar e não valem entre várias instâncias):
 * <ul>
 *   <li>envio: no máximo 20 por loja a cada 10 minutos ({@link #tentar});</li>
 *   <li>revisão e confirmação (que reprocessam o XML guardado): 60 por loja a cada 10 minutos
 *       ({@link #tentarRevisao});</li>
 *   <li>por IP: 120 chamadas a cada 10 minutos ({@link #tentarIp});</li>
 *   <li>global: 600 chamadas a cada 10 minutos ({@link #tentarGlobal});</li>
 *   <li>processamento simultâneo: no máximo 3 leituras de XML ao mesmo tempo ({@link #entrarProcessamento}).</li>
 * </ul>
 * Contadores de quem parou de chamar são removidos do mapa (não crescem sem limite).
 */
@Component
public class NfeLimitador {

    public static final int MAXIMO_PADRAO = 20;
    public static final int MAXIMO_REVISAO_PADRAO = 60;
    public static final int MAXIMO_IP_PADRAO = 120;
    public static final int MAXIMO_GLOBAL_PADRAO = 600;
    public static final int VAGAS_PADRAO = 3;
    public static final Duration JANELA_PADRAO = Duration.ofMinutes(10);
    private static final int PODA_A_CADA = 200;

    private static final class Janela {
        final int maximo;
        final long janelaMs;
        final Map<Object, Deque<Long>> filas = new ConcurrentHashMap<>();
        int chamadas;

        Janela(int maximo, long janelaMs) {
            this.maximo = maximo;
            this.janelaMs = janelaMs;
        }
    }

    private final LongSupplier relogioMs;
    private final Janela envio;
    private final Janela revisao;
    private final Janela ip;
    private final Janela global;
    private final Semaphore vagas;

    @Autowired
    public NfeLimitador() {
        this(MAXIMO_PADRAO, JANELA_PADRAO, System::currentTimeMillis);
    }

    /** Só o limite de envio por loja é configurável aqui; os demais usam os padrões. */
    public NfeLimitador(int maximo, Duration janela, LongSupplier relogioMs) {
        this(maximo, MAXIMO_REVISAO_PADRAO, MAXIMO_IP_PADRAO, MAXIMO_GLOBAL_PADRAO, VAGAS_PADRAO, janela, relogioMs);
    }

    public NfeLimitador(int maximoEnvio, int maximoRevisao, int maximoIp, int maximoGlobal, int vagas,
                        Duration janela, LongSupplier relogioMs) {
        this.relogioMs = relogioMs;
        long ms = janela.toMillis();
        this.envio = new Janela(maximoEnvio, ms);
        this.revisao = new Janela(maximoRevisao, ms);
        this.ip = new Janela(maximoIp, ms);
        this.global = new Janela(maximoGlobal, ms);
        this.vagas = new Semaphore(vagas);
    }

    /** Envio de XML por loja. Se passou do limite, a tentativa NÃO é contada. */
    public boolean tentar(Integer usuarioCodigo) {
        return permitir(envio, usuarioCodigo);
    }

    /** Revisão ou confirmação por loja. */
    public boolean tentarRevisao(Integer usuarioCodigo) {
        return permitir(revisao, usuarioCodigo);
    }

    /** Qualquer chamada de NF-e por endereço IP (nulo conta como um só). */
    public boolean tentarIp(String endereco) {
        return permitir(ip, endereco == null ? "?" : endereco);
    }

    /** Qualquer chamada de NF-e, de qualquer loja. */
    public boolean tentarGlobal() {
        return permitir(global, "global");
    }

    /** Reserva uma vaga de processamento simultâneo; quem recebe true DEVE chamar {@link #sairProcessamento()}. */
    public boolean entrarProcessamento() {
        return vagas.tryAcquire();
    }

    public void sairProcessamento() {
        vagas.release();
    }

    /** Segundos até o envio da loja voltar a ser permitido (cabeçalho Retry-After); no mínimo 1. */
    public long segundosParaLiberar(Integer usuarioCodigo) {
        Deque<Long> fila = envio.filas.get(usuarioCodigo);
        long agora = relogioMs.getAsLong();
        if (fila == null) {
            return 1;
        }
        synchronized (fila) {
            descartarAntigas(fila, agora, envio.janelaMs);
            Long maisAntiga = fila.peekFirst();
            return maisAntiga == null ? 1 : Math.max(1, (maisAntiga + envio.janelaMs - agora + 999) / 1000);
        }
    }

    /** Zera todos os contadores (uso em testes). */
    public void reiniciar() {
        envio.filas.clear();
        revisao.filas.clear();
        ip.filas.clear();
        global.filas.clear();
    }

    /** Quantas chaves ainda têm contador (uso em testes). */
    int tamanhoDosMapas() {
        return envio.filas.size() + revisao.filas.size() + ip.filas.size() + global.filas.size();
    }

    private boolean permitir(Janela janela, Object chave) {
        long agora = relogioMs.getAsLong();
        podarDeVezEmQuando(janela, agora);
        boolean permitido;
        Deque<Long> fila = janela.filas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            descartarAntigas(fila, agora, janela.janelaMs);
            permitido = fila.size() < janela.maximo;
            if (permitido) {
                fila.addLast(agora);
            }
        }
        return permitido;
    }

    /** A cada tantas chamadas remove do mapa as filas que já esvaziaram (quem parou de chamar). */
    private void podarDeVezEmQuando(Janela janela, long agora) {
        boolean podar;
        synchronized (janela) {
            podar = ++janela.chamadas >= PODA_A_CADA;
            if (podar) {
                janela.chamadas = 0;
            }
        }
        if (!podar) {
            return;
        }
        janela.filas.entrySet().removeIf(entrada -> {
            Deque<Long> fila = entrada.getValue();
            synchronized (fila) {
                descartarAntigas(fila, agora, janela.janelaMs);
                return fila.isEmpty();
            }
        });
    }

    private static void descartarAntigas(Deque<Long> fila, long agora, long janelaMs) {
        while (!fila.isEmpty() && agora - fila.peekFirst() >= janelaMs) {
            fila.pollFirst();
        }
    }
}
