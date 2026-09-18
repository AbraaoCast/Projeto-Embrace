package br.com.doalocal.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Garante que o banco esteja pronto antes de a aplicação receber requisições. */
@WebListener
public final class ApplicationContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(ApplicationContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            DatabaseInitializer.initialize();
            LOGGER.info(() -> "Banco do DoaLocal inicializado em " + DatabaseConfig.databasePath());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Falha ao inicializar o banco do DoaLocal.", exception);
            throw new IllegalStateException("A aplicação não pôde inicializar o banco de dados.", exception);
        }
    }
}
