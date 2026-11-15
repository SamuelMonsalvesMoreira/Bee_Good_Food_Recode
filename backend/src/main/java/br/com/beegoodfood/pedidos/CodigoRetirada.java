package br.com.beegoodfood.pedidos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/** Codigo curto que o cliente mostra no balcao; a loja o informa para registrar a retirada (DEC-040). */
final class CodigoRetirada {
    // Sem 0, 1, I, L e O: evita confusao ao ler em voz alta ou digitar.
    private static final String ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int TAMANHO = 6;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigoRetirada() { }

    static String novo() {
        var codigo = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) { codigo.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length()))); }
        return codigo.toString();
    }

    /** Ignora maiusculas, espacos e hifens digitados; comparacao em tempo constante. */
    static boolean confere(String esperado, String informado) {
        if (esperado == null || informado == null) { return false; }
        String normalizado = informado.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), normalizado.getBytes(StandardCharsets.UTF_8));
    }
}
