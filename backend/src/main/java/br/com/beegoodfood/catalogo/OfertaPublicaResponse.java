package br.com.beegoodfood.catalogo;

import java.math.BigDecimal;
import java.time.Instant;

/** Oferta compravel no catalogo publico. Datas em UTC; apresentar no fuso do estabelecimento. */
public record OfertaPublicaResponse(long id, long produtoId, String produtoNome, String produtoDescricao,
                                    Categoria categoria, BigDecimal precoOriginal, BigDecimal precoOferta,
                                    int estoqueDisponivel, Instant inicioVenda, Instant vendaAte, Instant retiradaAte,
                                    Estabelecimento estabelecimento) {
    public record Categoria(long id, String nome, String slug) { }

    public record Estabelecimento(long id, String nome, String logradouro, String numero, String complemento,
                                  String bairro, String cidade, String estado, String fusoIana) { }
}
