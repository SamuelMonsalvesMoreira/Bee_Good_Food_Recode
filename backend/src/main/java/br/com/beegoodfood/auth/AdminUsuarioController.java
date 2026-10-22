package br.com.beegoodfood.auth;

import br.com.beegoodfood.comum.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@SecurityRequirement(name = "bearerAuth")
public class AdminUsuarioController {
    private final UsuarioService usuarios;
    public AdminUsuarioController(UsuarioService usuarios) { this.usuarios = usuarios; }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar usuários (administrador)", description = "Somente campos do cadastro e papéis; nunca senha, hash ou token.")
    public PaginaResponse<UsuarioResponse> listar(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return usuarios.listar(pagina, tamanho);
    }
}
