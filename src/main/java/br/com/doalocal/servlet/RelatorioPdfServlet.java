package br.com.doalocal.servlet;

import br.com.doalocal.exception.PdfGenerationException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.RelatorioResultado;
import br.com.doalocal.service.RelatorioService;
import br.com.doalocal.util.RelatorioJson;
import br.com.doalocal.util.RelatorioPdfGenerator;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Gera o PDF em memória e o envia como download autenticado. */
@WebServlet("/relatorios/pdf")
public final class RelatorioPdfServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RelatorioPdfServlet.class.getName());
    private final RelatorioService relatorioService = new RelatorioService();
    private final RelatorioPdfGenerator pdfGenerator = new RelatorioPdfGenerator();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            RelatorioResultado report = relatorioService.generate(
                    request.getParameter("dataInicial"),
                    request.getParameter("dataFinal"),
                    request.getParameter("familiaId"),
                    request.getParameter("doacaoId")
            );
            byte[] pdf = pdfGenerator.generate(report);
            response.setContentType("application/pdf");
            response.setContentLength(pdf.length);
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=\"" + pdfGenerator.fileName(report.filtro()) + "\""
            );
            response.getOutputStream().write(pdf);
        } catch (ValidationException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    exception.getErrorCode(), exception.getMessage());
        } catch (PdfGenerationException exception) {
            LOGGER.log(Level.SEVERE, "Falha técnica na geração do PDF de Relatórios.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "pdf_generation_failed", "Não foi possível gerar o PDF.");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna na exportação do relatório.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "internal", "Não foi possível exportar o relatório.");
        }
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.reset();
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(RelatorioJson.error(code, message));
    }
}
