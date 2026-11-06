package br.com.beegoodfood.estabelecimentos;

/** Contrato publico entre modulos (catalogo, pedidos, indicadores). */
public record EstabelecimentoResumo(long id, long responsavelId, StatusEstabelecimento status, String nome,
                                    String logradouro, String numero, String complemento, String bairro,
                                    String cidade, String estado, String cep, String fusoIana) {
    public boolean ativo() { return status == StatusEstabelecimento.ATIVO; }

    static EstabelecimentoResumo from(Estabelecimento e) {
        return new EstabelecimentoResumo(e.id(), e.responsavelId(), e.status(), e.nome(), e.logradouro(), e.numero(),
                e.complemento(), e.bairro(), e.cidade(), e.estado(), e.cep(), e.fusoIana());
    }
}
