package br.com.doalocal.service;

import br.com.doalocal.dao.AdministradorDao;
import br.com.doalocal.exception.DuplicateEmailException;
import br.com.doalocal.exception.InvalidCredentialsException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Administrador;
import br.com.doalocal.model.AdministradorAutenticado;
import br.com.doalocal.util.PasswordHasher;
import br.com.doalocal.util.TextFormats;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Regras de cadastro, validação e autenticação de administradores. */
public final class AdministradorService {

    static final int MAX_NAME_LENGTH = 100;
    static final int MAX_EMAIL_LENGTH = 254;
    static final int MIN_PASSWORD_LENGTH = 8;
    static final int MAX_PASSWORD_LENGTH = 128;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HAS_LETTER = Pattern.compile(".*\\p{L}.*", Pattern.DOTALL);
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*", Pattern.DOTALL);

    private final AdministradorDao administradorDao;
    private final PasswordHasher passwordHasher;

    public AdministradorService() {
        this(new AdministradorDao(), new PasswordHasher());
    }

    public AdministradorService(AdministradorDao administradorDao, PasswordHasher passwordHasher) {
        this.administradorDao = administradorDao;
        this.passwordHasher = passwordHasher;
    }

    public AdministradorAutenticado register(
            String nome,
            String email,
            String senha,
            String confirmacaoSenha
    ) {
        validateRequiredRegistrationFields(nome, email, senha, confirmacaoSenha);

        String normalizedName = TextFormats.titleCase(nome.trim());
        String normalizedEmail = normalizeEmail(email);
        validateName(normalizedName);
        validateEmail(normalizedEmail);
        validatePassword(senha);

        if (!senha.equals(confirmacaoSenha)) {
            throw new ValidationException("password_mismatch", "As senhas não coincidem.");
        }

        if (administradorDao.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException();
        }

        String passwordHash = passwordHasher.hash(senha);
        Administrador administrador = administradorDao.insert(normalizedName, normalizedEmail, passwordHash);
        return AdministradorAutenticado.from(administrador);
    }

    public AdministradorAutenticado authenticate(String email, String senha) {
        if (isBlank(email) || isBlank(senha)) {
            throw new ValidationException("login_required", "Informe seu e-mail e senha.");
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.length() > MAX_EMAIL_LENGTH
                || senha.length() > MAX_PASSWORD_LENGTH
                || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new InvalidCredentialsException();
        }

        Optional<Administrador> administrador = administradorDao.findByEmail(normalizedEmail);
        if (administrador.isEmpty()
                || !passwordHasher.verify(senha, administrador.get().getSenhaHash())) {
            throw new InvalidCredentialsException();
        }

        return AdministradorAutenticado.from(administrador.get());
    }

    private static void validateRequiredRegistrationFields(
            String nome,
            String email,
            String senha,
            String confirmacaoSenha
    ) {
        if (isBlank(nome) || isBlank(email) || isBlank(senha) || isBlank(confirmacaoSenha)) {
            throw new ValidationException("required_fields", "Preencha todos os campos obrigatórios.");
        }
    }

    private static void validateName(String nome) {
        if (nome.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(
                    "name_too_long",
                    "O nome deve possuir no máximo " + MAX_NAME_LENGTH + " caracteres."
            );
        }
    }

    private static void validateEmail(String email) {
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("invalid_email", "Informe um e-mail válido.");
        }
    }

    private static void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH
                || !HAS_LETTER.matcher(password).matches()
                || !HAS_DIGIT.matcher(password).matches()) {
            throw new ValidationException(
                    "weak_password",
                    "A senha deve ter entre 8 e 128 caracteres, com pelo menos uma letra e um número."
            );
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
