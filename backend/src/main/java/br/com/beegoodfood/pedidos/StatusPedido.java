package br.com.beegoodfood.pedidos;

/**
 * Matriz de transicao (DEC-021). O responsavel avanca uma etapa por vez; CONCLUIDO e CANCELADO sao finais.
 * Cancelamento: cliente somente em PENDENTE; responsavel em qualquer estado nao final, com motivo.
 */
public enum StatusPedido {
    PENDENTE, CONFIRMADO, EM_PREPARO, PRONTO_PARA_RETIRADA, CONCLUIDO, CANCELADO;

    public boolean finalizado() { return this == CONCLUIDO || this == CANCELADO; }

    /** Proxima etapa do fluxo operacional, ou null quando nao ha avanco possivel. */
    public StatusPedido proximo() {
        return switch (this) {
            case PENDENTE -> CONFIRMADO;
            case CONFIRMADO -> EM_PREPARO;
            case EM_PREPARO -> PRONTO_PARA_RETIRADA;
            case PRONTO_PARA_RETIRADA -> CONCLUIDO;
            case CONCLUIDO, CANCELADO -> null;
        };
    }
}
