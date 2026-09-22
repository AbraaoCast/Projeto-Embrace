package br.com.doalocal.servlet;

import br.com.doalocal.exception.DoacaoNotFoundException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Doacao;
import br.com.doalocal.service.DoacaoService;
import br.com.doalocal.util.DoacaoJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Fornece listagem e consulta individual de doações em JSON. */
@WebServlet("/api/doacoes/*")
public final class DoacaoApiServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DoacaoApiServlet.class.getName());
    private final DoacaoService doacaoService = new DoacaoService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            String path = request.getPathInfo();
            if (path == null || path.equals("/") || path.isBlank()) {
                response.getWriter().write(DoacaoJson.list(doacaoService.listAll()));
                return;
            }

            String rawId = path.startsWith("/") ? path.substring(1) : path;
            Doacao doacao = doacaoService.findById(rawId);
            response.getWriter().write(DoacaoJson.object(doacao));
        } catch (ValidationException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    exception.getErrorCode(), exception.getMessage());
        } catch (DoacaoNotFoundException exception) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND,
                    "donation_not_found", exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna ao consultar doações.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "internal", "Não foi possível consultar as doações.");
        }
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.getWriter().write(DoacaoJson.error(code, message));
    }
}
