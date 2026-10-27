# Bibliotecas de terceiros (vendor)

Tudo o que está nesta pasta é de terceiros, **servido localmente** (sem CDN) e **não foi alterado**. Os arquivos foram
extraídos dos pacotes publicados no npm; antes de extrair, o hash SHA-512 de cada pacote (`.tgz`) foi calculado e
comparado com o campo `integrity` informado pelo registro do npm (`https://registry.npmjs.org/<pacote>/<versão>`).
Os pacotes conferiram. Os SHA-256 abaixo foram calculados sobre os arquivos desta pasta (`sha256sum`), e o do
arquivo da fonte também foi conferido com `certutil -hashfile ... SHA256`.

Data do download: 26/10/2026.

## Pacotes

| Biblioteca | Versão | Licença | Pacote npm | `integrity` do pacote (SHA-512, base64) |
|---|---|---|---|---|
| Bootstrap | 5.3.8 | MIT | `https://registry.npmjs.org/bootstrap/-/bootstrap-5.3.8.tgz` | `HP1SZDqaLDPwsNiqRqi5NcP0SSXciX2s9E+RyqJIIqGo+vJeN5AJVM98CXmW/Wux0nQ5L7jeWUdplCEf0Ee+tg==` |
| Bootstrap Icons | 1.11.3 | MIT | `https://registry.npmjs.org/bootstrap-icons/-/bootstrap-icons-1.11.3.tgz` | `+3lpHrCw/it2/7lBL15VR0HEumaBss0+f/Lb6ZvHISn1mlK83jjFpooTLsMWbIjJMDjDjOExMsTxnXSIT4k4ww==` |
| Inter (variável, subset latino) | 5.3.0 do pacote `@fontsource-variable/inter` (metadados do pacote: origem Google Fonts, "v20", eixo de peso 100 a 900) | SIL OFL 1.1 | `https://registry.npmjs.org/@fontsource-variable/inter/-/inter-5.3.0.tgz` | `OupL48va4JNofb97w6NYeF9S7W/kHNKM0Er8Dem5nqi4jeOLrVJDoE8tZEpnMJmtkvNbB1EIPPwHcdkF6b1oUA==` |
| Geist Mono (variável, eixo de peso, subset latino) | 5.3.0 do pacote `@fontsource-variable/geist-mono` (v3 do design, 26/10/2026) | SIL OFL 1.1 | `https://registry.npmjs.org/@fontsource-variable/geist-mono/-/geist-mono-5.3.0.tgz` | `vBbuwDEo9AkrqADMXOrlAR3DFcJi4/JxeuU43FoiQERnNwsfXNnvxvReZG02cQKmyk4DZkZdBZX3oTDvy2zBAw==` |

## Arquivos

| Arquivo (nesta pasta) | Origem dentro do pacote | Tamanho (bytes) | SHA-256 |
|---|---|---|---|
| `bootstrap-5.3.8/css/bootstrap.min.css` | `package/dist/css/bootstrap.min.css` | 232111 | `d85327d99c7a3ee1f9b5d0500d1370acea3ad2db39c163c2f51f232baedbdede` |
| `bootstrap-5.3.8/js/bootstrap.bundle.min.js` | `package/dist/js/bootstrap.bundle.min.js` | 80496 | `e4fd49181388c48ec5040bd3fe66f57c29c8e67fcd8502b3354b96ec7ab47cc7` |
| `bootstrap-5.3.8/LICENSE` | `package/LICENSE` | 1093 | `4620c84ad5ce8602ff65640ed6b7c8b78ebb9e036584f0ebc1ccc88206a4bb51` |
| `bootstrap-icons-1.11.3/bootstrap-icons.min.css` | `package/font/bootstrap-icons.min.css` | 85875 | `f643d6fe7e679f9de3e16311600c5ef5cd6b098f7a3a8828fcc29255d2b33e62` |
| `bootstrap-icons-1.11.3/fonts/bootstrap-icons.woff2` | `package/font/fonts/bootstrap-icons.woff2` | 130396 | `476adf42b40325098fcfa8b36ab3e769186bb4f6ce6a249753e2e1a9c22bf99e` |
| `bootstrap-icons-1.11.3/fonts/bootstrap-icons.woff` | `package/font/fonts/bootstrap-icons.woff` | 176032 | `bb1de989b83970f6f4e54de1cd974c5cba55b73582da5e1b225a6d0edf029483` |
| `bootstrap-icons-1.11.3/LICENSE` | `package/LICENSE` | 1093 | `0fb3e11bd57e896c5a512afd64864d28a37de45d19835016c87ca1ad19ead969` |
| `inter/inter-latin-wght-normal.woff2` | `package/files/inter-latin-wght-normal.woff2` | 48256 | `3100e775e8616cd2611beecfa23a4263d7037586789b43f035236a2e6fbd4c62` |
| `inter/OFL.txt` | `package/LICENSE` (texto da SIL OFL 1.1) | 4477 | `3b0a5fca3d17942cde889069889dedbbbd075e9b599968c82a95f4d944e9b345` |
| `geist-mono/geist-mono-latin-wght-normal.woff2` | `package/files/geist-mono-latin-wght-normal.woff2` | 23128 | `684ad5b531f81d43c1e8c7038262d5db7cdc1f68006e04d6c7769efa8d33c8cc` |
| `geist-mono/OFL.txt` | `package/LICENSE` (texto da SIL OFL 1.1) | 4498 | `cc815ed4fc045f0e991abb10395b7932bd028c6a067deb13316d6002105074e6` |

## Observações

- **Nenhum arquivo foi modificado.** Os dois arquivos do Bootstrap terminam com o comentário `sourceMappingURL`
  (`bootstrap.min.css.map` e `bootstrap.bundle.min.js.map`). Os mapas não foram baixados, então o navegador pode
  mostrar um aviso de 404 no DevTools (só com o DevTools aberto; não afeta o uso). Se incomodar, remover a última
  linha de cada arquivo e registrar aqui o novo hash.
- A fonte Inter vem do subset **latino** (`unicode-range` U+0000-00FF e outros, cobre todos os acentos do português,
  `R$` e as aspas tipográficas). O `@font-face` fica em `css/tokens.css` (família `Inter Variable`, `font-display: swap`).
- O CSS do Bootstrap Icons referencia `fonts/bootstrap-icons.woff2?<hash>` e `.woff` em caminho relativo; por isso a
  pasta `fonts/` fica ao lado do CSS.
- `/vendor/**` precisa estar liberado no `SecurityConfig` (já está) para a tela de login carregar estes arquivos
  sem sessão.
- **Para atualizar:** baixe o `.tgz` do novo pacote, confira o `integrity`, troque a pasta (o número da versão faz parte
  do nome), atualize as referências em `WEB-INF/tags/*.tag` e este arquivo, e recalcule os hashes.
- **Geist Mono (v3):** fonte monoespaçada usada **só** para valores, códigos, SKUs e atalhos (`--sh-font-mono`). Pacote baixado do registro do npm com `curl`; o SHA-512 do `.tgz` foi calculado com `openssl dgst -sha512 -binary | base64` e é **igual** ao `integrity` do registro. O `@font-face` (família `Geist Mono`, `font-display: swap`, mesmo `unicode-range` latino da Inter) fica em `css/tokens.css`. Arquivo não modificado. A Fraunces (v2) foi removida junto com a direção "caderneta de balcão".
