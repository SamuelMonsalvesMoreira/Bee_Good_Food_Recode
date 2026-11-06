package br.com.beegoodfood.estabelecimentos;

import br.com.beegoodfood.comum.Datas;
import java.time.Instant;

/** Visao privada: proprietario ou administrador. */
public record EstabelecimentoResponse(long id, long responsavelId, String nome, String descricao,
                                      StatusEstabelecimento status, Endereco endereco, Instant criadoEm) {
    public record Endereco(String logradouro, String numero, String complemento, String bairro,
                           String cidade, String estado, String cep, String fusoIana) { }

    static EstabelecimentoResponse from(Estabelecimento e) {
        return new EstabelecimentoResponse(e.id(), e.responsavelId(), e.nome(), e.descricao(), e.status(),
                new Endereco(e.logradouro(), e.numero(), e.complemento(), e.bairro(), e.cidade(), e.estado(),
                        e.cep(), e.fusoIana()), Datas.instante(e.criadoEm()));
    }
}
