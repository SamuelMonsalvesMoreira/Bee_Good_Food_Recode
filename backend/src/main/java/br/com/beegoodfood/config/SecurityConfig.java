package br.com.beegoodfood.config;

import br.com.beegoodfood.auth.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecretKey jwtKey(@Value("${bee.security.jwt-secret-base64}") String encoded) {
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException error) { throw new IllegalStateException("JWT_SECRET_BASE64 deve conter Base64 válido."); }
        if (decoded.length < 32) { throw new IllegalStateException("JWT_SECRET_BASE64 deve conter pelo menos 32 bytes aleatórios."); }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, Clock clock) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        OAuth2TokenValidator<Jwt> mandatory = jwt -> {
            boolean valid = jwt.getExpiresAt() != null && jwt.getIssuedAt() != null && jwt.getNotBefore() != null
                    && jwt.getSubject() != null && jwt.getSubject().matches("[1-9][0-9]{0,18}")
                    && jwt.getAudience() != null && jwt.getAudience().contains(TokenService.AUDIENCE)
                    && jwt.getIssuedAt().isBefore(jwt.getExpiresAt())
                    && !jwt.getIssuedAt().isAfter(clock.instant())
                    && jwt.getExpiresAt().isAfter(clock.instant())
                    && Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds() <= TokenService.VALIDADE_SEGUNDOS;
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp,
                new JwtIssuerValidator(TokenService.ISSUER), mandatory));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioService usuarios, Clock clock,
            @Value("${bee.security.auth-requests-per-minute}") int authLimit) throws Exception {
        return http
                // API stateless: sem cookie de autenticacao, sessao, login HTML ou HTTP Basic.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/cadastro", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categorias", "/api/v1/categorias/*",
                                "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, error) -> SecurityErrors.write(res, 401))
                        .accessDeniedHandler((req, res, error) -> SecurityErrors.write(res, 403)))
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint((req, res, error) -> SecurityErrors.write(res, 401))
                        .accessDeniedHandler((req, res, error) -> SecurityErrors.write(res, 403))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                            long id;
                            try { id = Long.parseLong(token.getSubject()); }
                            catch (NumberFormatException error) { throw new org.springframework.security.authentication.BadCredentialsException("Sessão inválida."); }
                            // Consultar status e papeis atuais: token antigo nao preserva privilegio removido.
                            var usuario = usuarios.ativo(id);
                            var authorities = usuario.papeis().stream().map(p -> new SimpleGrantedAuthority("ROLE_" + p)).toList();
                            return new JwtAuthenticationToken(token, authorities, Long.toString(id));
                        })))
                .addFilterBefore(new AuthRateLimitFilter(authLimit, clock), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
