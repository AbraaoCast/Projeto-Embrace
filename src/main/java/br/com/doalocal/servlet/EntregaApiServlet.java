package br.com.doalocal.servlet;

import br.com.doalocal.service.EntregaService;
import br.com.doalocal.service.FamiliaService;
import br.com.doalocal.util.EntregaJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Fornece histórico e opções reais para o formulário de entregas. */
@WebServlet("/api/entregas/*")
public final class EntregaApiServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(EntregaApiServlet.class.getName());
    private final EntregaService entregaService = new EntregaService();
    private final FamiliaService familiaService = new FamiliaService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            String path = request.getPathInfo();
            if (path == null || path.equals("/") || path.isBlank()) {
                response.getWriter().write(EntregaJson.history(entregaService.listHistory()));
            } else if (path.equals("/opcoes")) {
                response.getWriter().write(EntregaJson.options(
                        familiaService.listAll(), entregaService.listDonationAvailability()
                ));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(EntregaJson.error(
                        "not_found", "Recurso de entrega não encontrado."
                ));
            }
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna ao consultar entregas.", exception);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(EntregaJson.error(
                    "internal", "Não foi possível consultar as entregas."
            ));
        }
    }
}
