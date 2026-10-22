package br.com.beegoodfood.auth;

import java.util.List;

public record UsuarioResponse(long id, String nome, String email, boolean ativo, List<String> papeis) {
    static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.id(), usuario.nome(), usuario.email(), usuario.ativo(),
                usuario.papeis().stream().sorted().toList());
    }
}
