package br.com.doalocal.servlet;

import br.com.doalocal.exception.DuplicateCpfException;
import br.com.doalocal.exception.FamiliaNotFoundException;
import br.com.doalocal.exception.RelatedRecordsException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.service.FamiliaService;
import br.com.doalocal.util.FamiliaJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Processa criação, atualização e exclusão de famílias exclusivamente por POST. */
@WebServlet("/familias/*")
public final class FamiliaServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FamiliaServlet.class.getName());
    private final FamiliaService familiaService = new FamiliaService();

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
            familiaService.create(
                    parameter(request, "nomeResponsavel"), parameter(request, "cpf"),
                    parameter(request, "quantidadeMembros"), parameter(request, "telefone"),
                    parameter(request, "logradouro"), parameter(request, "numeroResidencia"),
                    parameter(request, "bairro"), parameter(request, "cidade")
            );
            redirect(response, request, "/gerenciar-fami.html?success=created");
        } catch (ValidationException exception) {
            redirect(response, request, "/cadastrar-fami.html?error=" + encode(exception.getErrorCode()));
        } catch (DuplicateCpfException exception) {
            redirect(response, request, "/cadastrar-fami.html?error=duplicate_cpf");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna no cadastro de família.", exception);
            redirect(response, request, "/cadastrar-fami.html?error=internal");
        }
    }

    private void update(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = parameter(request, "id");
        try {
            familiaService.update(
                    id, parameter(request, "nomeResponsavel"), parameter(request, "cpf"),
                    parameter(request, "quantidadeMembros"), parameter(request, "telefone"),
                    parameter(request, "logradouro"), parameter(request, "numeroResidencia"),
                    parameter(request, "bairro"), parameter(request, "cidade")
            );
            redirect(response, request, "/gerenciar-fami.html?success=updated");
        } catch (ValidationException exception) {
            redirectToEdit(response, request, id, exception.getErrorCode());
        } catch (DuplicateCpfException exception) {
            redirectToEdit(response, request, id, "duplicate_cpf");
        } catch (FamiliaNotFoundException exception) {
            redirect(response, request, "/gerenciar-fami.html?error=family_not_found");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna na atualização de família.", exception);
            redirectToEdit(response, request, id, "internal");
        }
    }

    private void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            familiaService.delete(parameter(request, "id"));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (ValidationException exception) {
            writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    exception.getErrorCode(), exception.getMessage());
        } catch (FamiliaNotFoundException exception) {
            writeJsonError(response, HttpServletResponse.SC_NOT_FOUND,
                    "family_not_found", exception.getMessage());
        } catch (RelatedRecordsException exception) {
            writeJsonError(response, HttpServletResponse.SC_CONFLICT,
                    "family_has_related_records", exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha interna na exclusão de família.", exception);
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "internal", "Não foi possível excluir a família.");
        }
    }

    private static String parameter(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    private static void redirectToEdit(
            HttpServletResponse response,
            HttpServletRequest request,
            String id,
            String errorCode
    ) throws IOException {
        redirect(response, request, "/editar-fami.html?id=" + encode(id) + "&error=" + encode(errorCode));
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
        response.getWriter().write(FamiliaJson.error(code, message));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
