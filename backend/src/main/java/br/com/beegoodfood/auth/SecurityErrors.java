package br.com.beegoodfood.auth;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class SecurityErrors {
    private SecurityErrors() { }

    public static void write(HttpServletResponse response, int status) throws IOException {
        String title = switch (status) {
            case 401 -> "Autenticação necessária";
            case 403 -> "Acesso negado";
            case 429 -> "Muitas tentativas. Aguarde um minuto.";
            default -> throw new IllegalArgumentException("Status de segurança inesperado");
        };
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (status == 401) { response.setHeader("WWW-Authenticate", "Bearer"); }
        if (status == 429) { response.setHeader("Retry-After", "60"); }
        // Somente constantes: nunca refletir credenciais ou conteudo recebido na resposta.
        response.getWriter().write("{\"type\":\"about:blank\",\"status\":" + status
                + ",\"title\":\"" + title + "\",\"detail\":\"" + title + "\"}");
    }
}
