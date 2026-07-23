package com.facturacion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Filtro de autenticación simple por API key para las rutas /api/**.
 *
 * Solo el backend (que conoce la clave) puede invocar al motor. Se compara la
 * cabecera "X-API-Key" contra la propiedad security.api-key (env MOTOR_API_KEY).
 *
 * Si la clave NO está configurada (vacía), el filtro NO exige nada: así el
 * despliegue no queda bloqueado hasta que se decida activar la clave en ambos
 * lados (backend y motor).
 *
 * No toca la lógica de comprobantes/XML/firma; solo intercepta la petición.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";

    @Value("${security.api-key:}")
    private String apiKeyConfigurada;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Sin clave configurada → no se exige nada (no bloquea).
        if (!StringUtils.hasText(apiKeyConfigurada)) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        // Solo protegemos la API; Swagger, health y docs quedan libres.
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String recibida = request.getHeader(HEADER);

        if (recibida != null && constanteIguales(recibida, apiKeyConfigurada)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"API key invalida o ausente\"}");
    }

    /** Comparación en tiempo constante para evitar timing attacks. */
    private boolean constanteIguales(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
