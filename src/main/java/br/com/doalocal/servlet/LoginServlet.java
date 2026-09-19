package br.com.doalocal.servlet;

import br.com.doalocal.config.AuthSession;
import br.com.doalocal.exception.InvalidCredentialsException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.AdministradorAutenticado;
import br.com.doalocal.service.AdministradorService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Autentica o administrador e cria uma sessão renovada. */
@WebServlet("/auth/login")
public final class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private final AdministradorService administradorService = new AdministradorService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        try {
            AdministradorAutenticado administrador = administradorService.authenticate(
                    request.getParameter("email"),
                    request.getParameter("senha")
            );

            HttpSession previousSession = request.getSession(false);
            if (previousSession != null) {
                previousSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setMaxInactiveInterval(AuthSession.TIMEOUT_SECONDS);
            session.setAttribute(AuthSession.ADMIN_ATTRIBUTE, administrador);

            response.sendRedirect(request.getContextPath() + "/dashboard.html");
        } catch (ValidationException exception) {
            redirectToLogin(response, request, exception.getErrorCode());
        } catch (InvalidCredentialsException exception) {
            redirectToLogin(response, request, "invalid_credentials");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna durante a autenticação.", exception);
            redirectToLogin(response, request, "internal");
        }
    }

    private static void redirectToLogin(
            HttpServletResponse response,
            HttpServletRequest request,
            String errorCode
    ) throws IOException {
        response.sendRedirect(
                request.getContextPath() + "/login-cadas.html?mode=login&error=" + errorCode
        );
    }
}
