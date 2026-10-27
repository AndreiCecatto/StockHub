<%@ tag description="Mascote do StockHub: a caixinha com rosto, em SVG inline desenhado à mão (estados: contente, atenta, preocupada, dormindo)" body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    Uso: <sh:mascote estado="contente" tamanho="g" />
      estado   contente (padrão), atenta, preocupada ou dormindo. O rosto inteiro vai no HTML e o CSS mostra o do estado
               atual (css/identidade.css); trocar o atributo data-estado do <svg> com JS muda a expressão sem recarregar.
      tamanho  p (pequeno, 72 px), m (médio, 120 px, padrão) ou g (grande, 176 px). A largura vem do CSS, sem style inline.
      rotulo   texto alternativo. Sem rótulo, o desenho é decorativo (aria-hidden), que é o caso comum.
      classe   classes extras no <svg>.
    As cores vêm dos tokens (--sh-kraft*, --sh-tinta, --sh-terra); o traço é levemente irregular de propósito.
    Os ids internos não existem (nada de gradiente nem filtro), então dá para repetir o mascote na mesma página.
--%>
<%@ attribute name="estado" required="false" type="java.lang.String" %>
<%@ attribute name="tamanho" required="false" type="java.lang.String" %>
<%@ attribute name="rotulo" required="false" type="java.lang.String" %>
<%@ attribute name="classe" required="false" type="java.lang.String" %>
<c:set var="est" value="${estado == 'atenta' or estado == 'preocupada' or estado == 'dormindo' ? estado : 'contente'}" />
<c:set var="tam" value="${tamanho == 'p' or tamanho == 'g' ? tamanho : 'm'}" />
<svg class="sh-mascote sh-mascote--<c:out value='${tam}'/> <c:out value='${classe}'/>" data-estado="<c:out value='${est}'/>"
    viewBox="0 0 200 190" focusable="false"
    <c:choose><c:when test="${not empty rotulo}">role="img" aria-label="<c:out value='${rotulo}'/>"</c:when><c:otherwise>aria-hidden="true"</c:otherwise></c:choose>>
    <ellipse class="mc-sombra" cx="100" cy="178" rx="62" ry="6"/>
    <g class="mc-corpo-todo">
        <%-- interior da caixa (parede de trás) e as duas abas --%>
        <path class="mc-interior" d="M50 86 L58 60 C82 55.5 120 55 142 59.5 L150 85 Z"/>
        <path class="mc-aba mc-aba--e" d="M46 86 L35 52 C34 48.5 37 46.5 41 47 L69 52 L73 86 Z"/>
        <path class="mc-aba mc-aba--d" d="M154 85 L165 51 C166 47.5 163 45.5 159 46 L131 51 L127 85 Z"/>
        <%-- frente da caixa --%>
        <path class="mc-frente" d="M42 85 C65 83.5 132 84.5 158 83 C161.5 106 160.5 143 158.5 168 C131 170 69 171 42.5 169 C39.5 142 40.5 106 42 85 Z"/>
        <path class="mc-borda" d="M42 85 C65 83.5 132 84.5 158 83 L157.6 94 C131 95.5 66 95 42.4 96.5 Z"/>
        <path class="mc-risco" d="M42.4 96.5 C66 95 131 95.5 157.6 94"/>
        <%-- etiqueta colada na frente --%>
        <g class="mc-etiqueta">
            <path class="mc-etiqueta__papel" d="M124 149 L151.5 147.6 L152.6 163.4 L125.2 164.8 Z"/>
            <path class="mc-etiqueta__linha" d="M129 154.4 L146.6 153.6 M129 159.4 L140.4 158.9"/>
        </g>
        <%-- rosto --%>
        <g class="mc-rosto">
            <ellipse class="mc-bochecha" cx="71" cy="137" rx="8" ry="4.8"/>
            <ellipse class="mc-bochecha" cx="129" cy="137" rx="8" ry="4.8"/>

            <g class="mc-olhos mc-olhos--abertos">
                <circle class="mc-olho" cx="82" cy="120" r="10"/>
                <circle class="mc-olho" cx="118" cy="120" r="10"/>
                <g class="mc-pupilas">
                    <circle class="mc-pupila" cx="82.6" cy="121" r="5"/>
                    <circle class="mc-pupila" cx="117.4" cy="121" r="5"/>
                    <circle class="mc-brilho" cx="84.4" cy="118.6" r="1.7"/>
                    <circle class="mc-brilho" cx="119.2" cy="118.6" r="1.7"/>
                </g>
            </g>
            <g class="mc-olhos mc-olhos--fechados">
                <path class="mc-traco" d="M71.5 121 C75 127.5 88 128 92.6 120.6"/>
                <path class="mc-traco" d="M107.6 120.8 C112 128 125 127.6 128.5 121.2"/>
            </g>

            <path class="mc-traco mc-testa mc-testa--atenta" d="M71.5 105 C76 101.5 86 100.4 92 103.6 M108.4 100.6 C113 96.4 123 96 128.6 99.6"/>
            <path class="mc-traco mc-testa mc-testa--preocupada" d="M70.6 109.6 C76 107.4 85 104.4 92.6 101.2 M107.4 101.4 C115 104.6 123.6 107.6 129.4 109.4"/>

            <path class="mc-boca mc-boca--contente" d="M84 141.6 C90 156.4 110.4 157.6 116.4 141 C106 143.8 94 143.6 84 141.6 Z"/>
            <path class="mc-boca mc-boca--atenta" d="M95 145.4 C95.4 140.6 105 140.2 105.2 145.2 C105.4 150.6 95.2 150.8 95 145.4 Z"/>
            <path class="mc-traco mc-boca--preocupada" d="M85 150 C90 141.6 95.6 142 100 147 C104.4 152 110.6 152 116 144"/>
            <path class="mc-traco mc-boca--dormindo" d="M92 144.4 C96 149 104.4 149.2 108.4 144"/>

            <path class="mc-gota" d="M141 96 C137 103.6 133.6 108 137.6 112.4 C141.4 116.4 148.6 112.6 146.2 106.4 C145 103 143.4 99.8 141 96 Z"/>
            <g class="mc-zzz">
                <path class="mc-traco" d="M150 78 L160 78 L150 90 L161 90"/>
                <path class="mc-traco mc-zzz__2" d="M168 58 L175.4 58 L168 66.6 L176 66.6"/>
            </g>
        </g>
    </g>
</svg>
