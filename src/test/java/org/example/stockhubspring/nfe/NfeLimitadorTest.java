package org.example.stockhubspring.nfe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class NfeLimitadorTest {

    @Test
    @DisplayName("permite até o máximo e bloqueia a próxima tentativa na janela")
    void bloqueiaAcimaDoMaximo() {
        NfeLimitador limitador = new NfeLimitador(3, Duration.ofMinutes(10), () -> 0L);
        assertThat(limitador.tentar(1)).isTrue();
        assertThat(limitador.tentar(1)).isTrue();
        assertThat(limitador.tentar(1)).isTrue();
        assertThat(limitador.tentar(1)).isFalse();
        assertThat(limitador.tentar(1)).isFalse();
    }

    @Test
    @DisplayName("cada loja tem o seu contador")
    void lojasIndependentes() {
        NfeLimitador limitador = new NfeLimitador(1, Duration.ofMinutes(10), () -> 0L);
        assertThat(limitador.tentar(1)).isTrue();
        assertThat(limitador.tentar(1)).isFalse();
        assertThat(limitador.tentar(2)).isTrue();
    }

    @Test
    @DisplayName("passada a janela, volta a permitir")
    void janelaDeslizante() {
        AtomicLong agora = new AtomicLong(0);
        NfeLimitador limitador = new NfeLimitador(2, Duration.ofMinutes(10), agora::get);
        assertThat(limitador.tentar(1)).isTrue();
        agora.set(Duration.ofMinutes(5).toMillis());
        assertThat(limitador.tentar(1)).isTrue();
        assertThat(limitador.tentar(1)).isFalse();
        assertThat(limitador.segundosParaLiberar(1)).isEqualTo(300);
        agora.set(Duration.ofMinutes(10).toMillis());
        assertThat(limitador.tentar(1)).isTrue();
        assertThat(limitador.tentar(1)).isFalse();
    }

    @Test
    @DisplayName("o padrão é 20 envios a cada 10 minutos")
    void padrao() {
        NfeLimitador limitador = new NfeLimitador();
        for (int i = 0; i < 20; i++) {
            assertThat(limitador.tentar(7)).as("envio %d", i + 1).isTrue();
        }
        assertThat(limitador.tentar(7)).isFalse();
    }

    @Test
    @DisplayName("limite por IP, global e de revisão são independentes do de envio")
    void outrosLimites() {
        NfeLimitador limitador = new NfeLimitador(5, 2, 1, 3, 2, Duration.ofMinutes(10), () -> 0L);

        assertThat(limitador.tentarIp("1.1.1.1")).isTrue();
        assertThat(limitador.tentarIp("1.1.1.1")).isFalse();
        assertThat(limitador.tentarIp("2.2.2.2")).isTrue();

        assertThat(limitador.tentarRevisao(1)).isTrue();
        assertThat(limitador.tentarRevisao(1)).isTrue();
        assertThat(limitador.tentarRevisao(1)).isFalse();
        assertThat(limitador.tentar(1)).isTrue();

        assertThat(limitador.tentarGlobal()).isTrue();
        assertThat(limitador.tentarGlobal()).isTrue();
        assertThat(limitador.tentarGlobal()).isTrue();
        assertThat(limitador.tentarGlobal()).isFalse();
    }

    @Test
    @DisplayName("processamento simultâneo: só as vagas existem e quem sai libera")
    void vagasDeProcessamento() {
        NfeLimitador limitador = new NfeLimitador(5, 2, 2, 2, 2, Duration.ofMinutes(10), () -> 0L);

        assertThat(limitador.entrarProcessamento()).isTrue();
        assertThat(limitador.entrarProcessamento()).isTrue();
        assertThat(limitador.entrarProcessamento()).isFalse();
        limitador.sairProcessamento();
        assertThat(limitador.entrarProcessamento()).isTrue();
    }

    @Test
    @DisplayName("contadores de quem parou de chamar saem do mapa")
    void podaDoMapa() {
        AtomicLong agora = new AtomicLong(0);
        NfeLimitador limitador = new NfeLimitador(5, 5, 100_000, 100_000, 3, Duration.ofMinutes(10), agora::get);
        for (int i = 0; i < 150; i++) {
            limitador.tentarIp("10.0.0." + i);
        }
        assertThat(limitador.tamanhoDosMapas()).isGreaterThanOrEqualTo(150);

        agora.set(Duration.ofMinutes(11).toMillis());
        for (int i = 0; i < 300; i++) {
            limitador.tentarIp("20.0.0." + (i % 2));
        }

        // os 150 antigos já venceram e foram podados; sobram só os 2 ativos (e a fila global vazia)
        assertThat(limitador.tamanhoDosMapas()).isLessThan(10);
    }

    @Test
    @DisplayName("reiniciar zera os contadores")
    void reiniciar() {
        NfeLimitador limitador = new NfeLimitador(1, Duration.ofMinutes(10), () -> 0L);
        assertThat(limitador.tentar(1)).isTrue();
        assertThat(limitador.tentar(1)).isFalse();
        limitador.reiniciar();
        assertThat(limitador.tentar(1)).isTrue();
    }
}
