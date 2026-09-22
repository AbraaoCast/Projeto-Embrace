package br.com.doalocal.servlet;

import br.com.doalocal.exception.DoacaoNotFoundException;
import br.com.doalocal.exception.FamiliaNotFoundException;
import br.com.doalocal.exception.InsufficientDonationQuantityException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.service.EntregaService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Registra uma entrega por POST e aplica o padrão POST-Redirect-GET. */
@WebServlet("/entregas")
public final class EntregaServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(EntregaServlet.class.getName());
    private final EntregaService entregaService = new EntregaService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            entregaService.register(
                    request.getParameter("familiaId"),
                    request.getParameter("doacaoId"),
                    request.getParameter("quantidadeEntregue"),
                    request.getParameter("dataEntrega"),
                    request.getParameter("observacao")
            );
            redirect(response, request, "/consultar-entregas.html?success=created");
        } catch (ValidationException exception) {
            redirectError(response, request, exception.getErrorCode());
        } catch (FamiliaNotFoundException exception) {
            redirectError(response, request, "family_not_found");
        } catch (DoacaoNotFoundException exception) {
            redirectError(response, request, "donation_not_found");
        } catch (InsufficientDonationQuantityException exception) {
            redirectError(response, request, "insufficient_quantity");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna no registro de entrega.", exception);
            redirectError(response, request, "internal");
        }
    }

    private static void redirectError(
            HttpServletResponse response,
            HttpServletRequest request,
            String errorCode
    ) throws IOException {
        redirect(response, request,
                "/registrar-entrega.html?error=" + URLEncoder.encode(
                        errorCode, StandardCharsets.UTF_8
                ));
    }

    private static void redirect(
            HttpServletResponse response,
            HttpServletRequest request,
            String location
    ) throws IOException {
        response.sendRedirect(request.getContextPath() + location);
    }
}
