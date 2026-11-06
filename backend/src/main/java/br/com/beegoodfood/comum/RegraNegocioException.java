package br.com.beegoodfood.comum;

import java.util.Map;

/** Violacao de regra de negocio com mensagem segura para o usuario (nunca reflete entrada do cliente). */
public class RegraNegocioException extends RuntimeException {
    private final int status;
    private final String titulo;
    private final Map<String, String> campos;

    private RegraNegocioException(int status, String titulo, String detalhe, Map<String, String> campos) {
        super(detalhe);
        this.status = status;
        this.titulo = titulo;
        this.campos = campos == null ? Map.of() : Map.copyOf(campos);
    }

    /** 409: o estado atual do recurso nao permite a operacao. */
    public static RegraNegocioException conflito(String detalhe) {
        return new RegraNegocioException(409, "Operação não permitida", detalhe, null);
    }

    /** 400: campos validos isoladamente, mas invalidos para a regra (ex.: periodo vencido). */
    public static RegraNegocioException invalido(String campo, String mensagem) {
        return new RegraNegocioException(400, "Dados inválidos", "Confira os campos informados.", Map.of(campo, mensagem));
    }

    public static RegraNegocioException invalido(Map<String, String> campos) {
        return new RegraNegocioException(400, "Dados inválidos", "Confira os campos informados.", campos);
    }

    public int status() { return status; }
    public String titulo() { return titulo; }
    public Map<String, String> campos() { return campos; }
}
