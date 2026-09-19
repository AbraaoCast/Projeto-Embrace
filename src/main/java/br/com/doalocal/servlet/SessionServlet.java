package br.com.doalocal.servlet;

import br.com.doalocal.config.AuthSession;
import br.com.doalocal.model.AdministradorAutenticado;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Fornece ao frontend somente os dados não sensíveis do administrador da sessão. */
@WebServlet("/api/session")
public final class SessionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AdministradorAutenticado administrador = (AdministradorAutenticado) request.getSession(false)
                .getAttribute(AuthSession.ADMIN_ATTRIBUTE);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().printf(
                "{\"id\":%d,\"nome\":\"%s\",\"email\":\"%s\"}",
                administrador.id(),
                escapeJson(administrador.nome()),
                escapeJson(administrador.email())
        );
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
