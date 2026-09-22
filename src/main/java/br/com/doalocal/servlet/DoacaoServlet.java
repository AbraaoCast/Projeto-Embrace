package br.com.doalocal.servlet;

import br.com.doalocal.exception.DoacaoNotFoundException;
import br.com.doalocal.exception.RelatedDonationRecordsException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.service.DoacaoService;
import br.com.doalocal.util.DoacaoJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Processa criação, atualização e exclusão de doações exclusivamente por POST. */
@WebServlet("/doacoes/*")
public final class DoacaoServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DoacaoServlet.class.getName());
    private final DoacaoService doacaoService = new DoacaoService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getPathInfo();

        if (action == null || action.equals("/")) {
            create(request, response);
        } else if (action.equals("/update")) {
            update(request, response);
        } else if (action.equals("/delete")) {
            delete(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void create(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            doacaoService.create(
                    request.getParameter("tipo"), request.getParameter("descricao"),
                    request.getParameter("quantidade"), request.getParameter("dataDoacao"),
                    request.getParameter("observacao")
            );
            redirect(response, request, "/gerenciar-doacoes.html?success=created");
        } catch (ValidationException exception) {
            redirect(response, request, "/cadastrar-doacao.html?error=" + encode(exception.getErrorCode()));
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna no cadastro de doação.", exception);
            redirect(response, request, "/cadastrar-doacao.html?error=internal");
        }
    }

    private void update(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter("id");
        try {
            doacaoService.update(
                    id, request.getParameter("tipo"), request.getParameter("descricao"),
                    request.getParameter("quantidade"), request.getParameter("dataDoacao"),
                    request.getParameter("observacao")
            );
            redirect(response, request, "/gerenciar-doacoes.html?success=updated");
        } catch (ValidationException exception) {
            redirectToEdit(response, request, id, exception.getErrorCode());
        } catch (DoacaoNotFoundException exception) {
            redirect(response, request, "/gerenciar-doacoes.html?error=donation_not_found");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna na atualização de doação.", exception);
            redirectToEdit(response, request, id, "internal");
        }
    }

    private void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            doacaoService.delete(request.getParameter("id"));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (ValidationException exception) {
            writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    exception.getErrorCode(), exception.getMessage());
        } catch (DoacaoNotFoundException exception) {
            writeJsonError(response, HttpServletResponse.SC_NOT_FOUND,
                    "donation_not_found", exception.getMessage());
        } catch (RelatedDonationRecordsException exception) {
            writeJsonError(response, HttpServletResponse.SC_CONFLICT,
                    "donation_has_related_records", exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna na exclusão de doação.", exception);
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "internal", "Não foi possível excluir a doação.");
        }
    }

    private static void redirectToEdit(
            HttpServletResponse response,
            HttpServletRequest request,
            String id,
            String errorCode
    ) throws IOException {
        redirect(response, request,
                "/editar-doacao.html?id=" + encode(id) + "&error=" + encode(errorCode));
    }

    private static void redirect(
            HttpServletResponse response,
            HttpServletRequest request,
            String location
    ) throws IOException {
        response.sendRedirect(request.getContextPath() + location);
    }

    private static void writeJsonError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.getWriter().write(DoacaoJson.error(code, message));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
