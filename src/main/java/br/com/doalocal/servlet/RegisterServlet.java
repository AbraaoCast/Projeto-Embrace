package br.com.doalocal.servlet;

import br.com.doalocal.exception.DuplicateEmailException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.service.AdministradorService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Recebe o cadastro público do administrador exclusivamente por POST. */
@WebServlet("/auth/register")
public final class RegisterServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());
    private final AdministradorService administradorService = new AdministradorService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        try {
            administradorService.register(
                    request.getParameter("nome"),
                    request.getParameter("email"),
                    request.getParameter("senha"),
                    request.getParameter("confSenha")
            );
            redirect(response, request, "mode=login&success=registered");
        } catch (ValidationException exception) {
            redirect(response, request, "mode=register&error=" + exception.getErrorCode());
        } catch (DuplicateEmailException exception) {
            redirect(response, request, "mode=register&error=duplicate_email");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna no cadastro de administrador.", exception);
            redirect(response, request, "mode=register&error=internal");
        }
    }

    private static void redirect(HttpServletResponse response, HttpServletRequest request, String query)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login-cadas.html?" + query);
    }
}
