package br.com.doalocal.servlet;

import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.service.RelatorioService;
import br.com.doalocal.util.RelatorioJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Fornece opções e resultados consolidados para a tela de Relatórios. */
@WebServlet("/api/relatorios/*")
public final class RelatorioApiServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RelatorioApiServlet.class.getName());
    private final RelatorioService relatorioService = new RelatorioService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            String path = request.getPathInfo();
            if (path == null || path.equals("/") || path.isBlank()) {
                response.getWriter().write(RelatorioJson.result(relatorioService.generate(
                        request.getParameter("dataInicial"),
                        request.getParameter("dataFinal"),
                        request.getParameter("familiaId"),
                        request.getParameter("doacaoId")
                )));
            } else if (path.equals("/opcoes")) {
                response.getWriter().write(RelatorioJson.options(relatorioService.listOptions()));
            } else {
                writeError(response, HttpServletResponse.SC_NOT_FOUND,
                        "not_found", "Recurso de relatório não encontrado.");
            }
        } catch (ValidationException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    exception.getErrorCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna na consulta de relatórios.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "internal", "Não foi possível gerar o relatório.");
        }
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.getWriter().write(RelatorioJson.error(code, message));
    }
}
