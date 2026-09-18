package br.com.doalocal.servlet;

import br.com.doalocal.config.ConnectionFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Endpoint técnico mínimo usado para verificar aplicação e banco. */
@WebServlet("/api/health")
public final class HealthServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HealthServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try (Connection connection = ConnectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1")) {
            if (!result.next()) {
                throw new SQLException("A consulta de verificação não retornou resultado.");
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\":\"ok\",\"database\":\"connected\"}");
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Falha na verificação de saúde do banco.", exception);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"status\":\"unavailable\"}");
        }
    }
}
