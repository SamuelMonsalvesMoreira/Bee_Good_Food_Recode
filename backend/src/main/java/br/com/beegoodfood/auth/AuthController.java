package br.com.beegoodfood.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UsuarioService usuarios;
    private final TokenService tokens;

    public AuthController(UsuarioService usuarios, TokenService tokens) { this.usuarios = usuarios; this.tokens = tokens; }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar cliente adulto", description = "Autodeclaração obrigatória. Somente papel CLIENTE; senha de 12 a 72 caracteres, limitada a 72 bytes UTF-8.")
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest request) { return usuarios.cadastrar(request); }

    @PostMapping("/login")
    @Operation(summary = "Entrar", description = "JWT válido por 15 minutos; não existe refresh token. Guardar apenas em memória no frontend.")
    public TokenService.TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return tokens.emitir(usuarios.autenticar(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar a própria conta ativa")
    @SecurityRequirement(name = "bearerAuth")
    public UsuarioResponse me(Authentication authentication) {
        return usuarios.ativo(Long.parseLong(authentication.getName()));
    }
}
