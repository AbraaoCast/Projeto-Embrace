package br.com.doalocal.servlet;

import br.com.doalocal.service.DashboardService;
import br.com.doalocal.util.DashboardJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Entrega a visão geral consolidada para o Dashboard autenticado. */
@WebServlet("/api/dashboard")
public final class DashboardApiServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DashboardApiServlet.class.getName());
    private final DashboardService dashboardService = new DashboardService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            response.getWriter().write(DashboardJson.result(dashboardService.load()));
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna ao carregar o Dashboard.", exception);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(DashboardJson.error(
                    "internal", "Não foi possível carregar o Dashboard."
            ));
        }
    }
}
