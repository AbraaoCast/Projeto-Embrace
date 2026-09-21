package br.com.doalocal.servlet;

import br.com.doalocal.exception.FamiliaNotFoundException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Familia;
import br.com.doalocal.service.FamiliaService;
import br.com.doalocal.util.FamiliaJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Fornece listagem e consulta individual de famílias em JSON. */
@WebServlet("/api/familias/*")
public final class FamiliaApiServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FamiliaApiServlet.class.getName());
    private final FamiliaService familiaService = new FamiliaService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            String path = request.getPathInfo();
            if (path == null || path.equals("/") || path.isBlank()) {
                response.getWriter().write(FamiliaJson.list(familiaService.listAll()));
                return;
            }

            String rawId = path.startsWith("/") ? path.substring(1) : path;
            Familia familia = familiaService.findById(rawId);
            response.getWriter().write(FamiliaJson.object(familia));
        } catch (ValidationException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    exception.getErrorCode(), exception.getMessage());
        } catch (FamiliaNotFoundException exception) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND,
                    "family_not_found", exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna ao consultar famílias.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "internal", "Não foi possível consultar as famílias.");
        }
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.getWriter().write(FamiliaJson.error(code, message));
    }
}
