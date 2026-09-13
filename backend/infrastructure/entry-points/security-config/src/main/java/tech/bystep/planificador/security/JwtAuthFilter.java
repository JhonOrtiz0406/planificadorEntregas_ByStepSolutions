package tech.bystep.planificador.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TenantAccessVerifier tenantAccessVerifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        boolean accessRevoked = false;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtService.isTokenValid(token) && !jwtService.isSelectionToken(token)) {
                    String email = jwtService.extractEmail(token);
                    String role = jwtService.extractRole(token);
                    String userId = jwtService.extractUserId(token).toString();
                    String orgId = jwtService.extractOrganizationId(token);

                    if (tenantAccessVerifier.hasAccess(userId, role, orgId)) {
                        UserPrincipal principal = new UserPrincipal(userId, email, role, orgId);
                        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        // Aislamiento: usuario inhabilitado, sacado de la organización o con otro rol.
                        accessRevoked = !isPublicPath(request.getRequestURI());
                        log.info("Acceso revocado para el usuario {} en la organización {}", userId, orgId);
                    }
                }
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
            }
        }

        if (accessRevoked) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":"
                    + "\"Tu acceso a esta organización fue revocado o cambió. Inicia sesión nuevamente.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Rutas públicas: con un token revocado se atienden como anónimas (no se responde 401). */
    private static boolean isPublicPath(String uri) {
        return uri.startsWith("/api/auth/")
                || uri.startsWith("/api/invitations/")
                || uri.startsWith("/actuator/");
    }
}
