package br.com.beegoodfood.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limite local por IP, sem confiar em X-Forwarded-For enviado pelo cliente. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Window(long until, int count) { }
    private final Map<String, Window> attempts = new HashMap<>();
    private final Clock clock;
    private final int limit;

    public AuthRateLimitFilter(int limit, Clock clock) {
        if (limit < 1) { throw new IllegalArgumentException("Limite deve ser positivo"); }
        this.limit = limit;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") || !(request.getServletPath().equals("/api/v1/auth/login")
                || request.getServletPath().equals("/api/v1/auth/cadastro"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!allow(request.getRemoteAddr())) { SecurityErrors.write(response, 429); return; }
        chain.doFilter(request, response);
    }

    private synchronized boolean allow(String address) {
        long now = clock.millis();
        Window window = attempts.get(address);
        if (window != null && window.until() > now) {
            if (window.count() >= limit) { return false; }
            attempts.put(address, new Window(window.until(), window.count() + 1));
            return true;
        }
        attempts.entrySet().removeIf(entry -> entry.getValue().until() <= now);
        // Memoria limitada; falha fechada quando o limite de enderecos ativos e atingido.
        if (attempts.size() >= 10000) { return false; }
        attempts.put(address, new Window(now + 60000, 1));
        return true;
    }
}
