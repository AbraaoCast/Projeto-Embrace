package br.com.doalocal.filter;

import br.com.doalocal.config.AuthSession;
import br.com.doalocal.model.AdministradorAutenticado;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/** Protege por padrão os recursos que não fazem parte da lista pública. */
@WebFilter("/*")
public final class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/",
            "/login-cadas.html",
            "/auth/login",
            "/auth/register",
            "/api/health"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = requestPath(httpRequest);

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        Object administrator = session == null ? null : session.getAttribute(AuthSession.ADMIN_ATTRIBUTE);
        if (administrator instanceof AdministradorAutenticado) {
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/")) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"authentication_required\"}");
            return;
        }

        httpResponse.sendRedirect(httpRequest.getContextPath() + "/login-cadas.html?error=session_required");
    }

    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return uri.substring(contextPath.length());
    }

    private static boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path)
                || path.startsWith("/style/")
                || path.startsWith("/script/")
                || path.startsWith("/images/");
    }
}
