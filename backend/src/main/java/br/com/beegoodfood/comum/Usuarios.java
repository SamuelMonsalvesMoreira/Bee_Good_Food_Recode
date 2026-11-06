package br.com.beegoodfood.comum;

import org.springframework.security.core.Authentication;

public final class Usuarios {
    private Usuarios() { }

    /** O subject do token ja foi validado e convertido no filtro JWT. */
    public static long id(Authentication authentication) { return Long.parseLong(authentication.getName()); }

    public static boolean temPapel(Authentication authentication, String papel) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + papel));
    }
}
