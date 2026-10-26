package org.example.stockhubspring.controller;

import org.example.stockhubspring.service.ExportacaoService.ArquivoCsv;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Monta a resposta HTTP de download de um CSV (UTF-8, anexo com nome com data). */
final class RespostaCsv {

    /** Cabeçalho que avisa que o arquivo foi cortado pelo limite de linhas. */
    static final String CABECALHO_TRUNCADO = "X-Exportacao-Truncada";

    private static final MediaType TEXT_CSV_UTF8 = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private RespostaCsv() {
    }

    static ResponseEntity<byte[]> de(ArquivoCsv arquivo) {
        ResponseEntity.BodyBuilder resposta = ResponseEntity.ok()
                .contentType(TEXT_CSV_UTF8)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome()).build().toString());
        if (arquivo.truncado()) {
            resposta.header(CABECALHO_TRUNCADO, "true");
        }
        return resposta.body(arquivo.conteudo());
    }
}
