package br.com.doalocal.service;

import br.com.doalocal.dao.FamiliaDao;
import br.com.doalocal.exception.DuplicateCpfException;
import br.com.doalocal.exception.FamiliaNotFoundException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Familia;
import br.com.doalocal.util.PhoneFormats;
import br.com.doalocal.util.TextFormats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Valida e coordena as regras de negócio do gerenciamento de famílias. */
public final class FamiliaService {

    public static final int MAX_NOME_LENGTH = 100;
    public static final int MAX_MEMBROS = 100;
    public static final int MAX_LOGRADOURO_LENGTH = 150;
    public static final int MAX_NUMERO_LENGTH = 20;
    public static final int MAX_BAIRRO_LENGTH = 100;
    public static final int MAX_CIDADE_LENGTH = 100;

    private final FamiliaDao familiaDao;

    public FamiliaService() {
        this(new FamiliaDao());
    }

    public FamiliaService(FamiliaDao familiaDao) {
        this.familiaDao = familiaDao;
    }

    public Familia create(
            String nome,
            String cpf,
            String quantidadeMembros,
            String telefone,
            String logradouro,
            String numeroResidencia,
            String bairro,
            String cidade
    ) {
        FamilyData data = validateAndNormalize(
                nome, cpf, quantidadeMembros, telefone, logradouro, numeroResidencia, bairro, cidade
        );

        if (familiaDao.findByCpf(data.cpf()).isPresent()) {
            throw new DuplicateCpfException();
        }

        return familiaDao.insert(
                data.cpf(), data.nome(), data.quantidadeMembros(), data.telefone(),
                data.logradouro(), data.numeroResidencia(), data.bairro(), data.cidade(),
                LocalDateTime.now().withNano(0)
        );
    }

    public List<Familia> listAll() {
        return familiaDao.findAll();
    }

    public Familia findById(String rawId) {
        return findById(parseId(rawId));
    }

    public Familia findById(long id) {
        validatePositiveId(id);
        return familiaDao.findById(id).orElseThrow(FamiliaNotFoundException::new);
    }

    public Familia update(
            String rawId,
            String nome,
            String cpf,
            String quantidadeMembros,
            String telefone,
            String logradouro,
            String numeroResidencia,
            String bairro,
            String cidade
    ) {
        long id = parseId(rawId);
        Familia current = findById(id);
        FamilyData data = validateAndNormalize(
                nome, cpf, quantidadeMembros, telefone, logradouro, numeroResidencia, bairro, cidade
        );

        Optional<Familia> sameCpf = familiaDao.findByCpf(data.cpf());
        if (sameCpf.isPresent() && sameCpf.get().getId() != id) {
            throw new DuplicateCpfException();
        }

        if (!familiaDao.update(
                id, data.cpf(), data.nome(), data.quantidadeMembros(), data.telefone(),
                data.logradouro(), data.numeroResidencia(), data.bairro(), data.cidade()
        )) {
            throw new FamiliaNotFoundException();
        }

        Familia updated = familiaDao.findById(id).orElseThrow(FamiliaNotFoundException::new);
        if (!updated.getDataCadastro().equals(current.getDataCadastro())) {
            throw new IllegalStateException("A data original de cadastro foi alterada indevidamente.");
        }
        return updated;
    }

    public void delete(String rawId) {
        long id = parseId(rawId);
        findById(id);
        if (!familiaDao.delete(id)) {
            throw new FamiliaNotFoundException();
        }
    }

    static String normalizeCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    static boolean isValidCpf(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = cpfDigit(cpf, 9, 10);
        int secondDigit = cpfDigit(cpf, 10, 11);
        return firstDigit == Character.digit(cpf.charAt(9), 10)
                && secondDigit == Character.digit(cpf.charAt(10), 10);
    }

    private static int cpfDigit(String cpf, int length, int initialWeight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.digit(cpf.charAt(index), 10) * (initialWeight - index);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static FamilyData validateAndNormalize(
            String nome,
            String cpf,
            String quantidadeMembros,
            String telefone,
            String logradouro,
            String numeroResidencia,
            String bairro,
            String cidade
    ) {
        String normalizedName = TextFormats.titleCase(
                required(nome, "name_required", "Informe o nome do responsável.")
        );
        String normalizedCpf = required(cpf, "cpf_required", "Informe o CPF do responsável.");
        String normalizedQuantity = required(
                quantidadeMembros, "members_required", "Informe a quantidade de membros."
        );
        String requiredPhone = required(telefone, "phone_required", "Informe o telefone.");
        String normalizedStreet = TextFormats.titleCase(
                required(logradouro, "address_required", "Informe o endereço.")
        );
        String normalizedDistrict = TextFormats.titleCase(
                required(bairro, "district_required", "Informe o bairro.")
        );
        String normalizedCity = TextFormats.titleCase(
                required(cidade, "city_required", "Informe a cidade.")
        );
        String normalizedNumber = optional(numeroResidencia);

        normalizedCpf = normalizeCpf(normalizedCpf);
        if (!isValidCpf(normalizedCpf)) {
            throw new ValidationException("invalid_cpf", "Informe um CPF válido.");
        }

        String normalizedPhone = PhoneFormats.normalizeMobile(requiredPhone);
        if (normalizedPhone == null) {
            throw new ValidationException(
                    "invalid_phone", "Informe um telefone válido no formato (98) 99999-9999."
            );
        }

        int members;
        try {
            members = Integer.parseInt(normalizedQuantity);
        } catch (NumberFormatException exception) {
            throw new ValidationException("invalid_members", "Informe uma quantidade de membros válida.");
        }
        if (members <= 0) {
            throw new ValidationException(
                    "members_must_be_positive", "A quantidade de membros deve ser maior que zero."
            );
        }
        if (members > MAX_MEMBROS) {
            throw new ValidationException(
                    "members_too_large", "A quantidade de membros deve ser no máximo " + MAX_MEMBROS + "."
            );
        }

        validateLength(normalizedName, MAX_NOME_LENGTH, "name_too_long", "nome do responsável");
        validateLength(normalizedStreet, MAX_LOGRADOURO_LENGTH, "address_too_long", "endereço");
        validateLength(normalizedNumber, MAX_NUMERO_LENGTH, "number_too_long", "número");
        validateLength(normalizedDistrict, MAX_BAIRRO_LENGTH, "district_too_long", "bairro");
        validateLength(normalizedCity, MAX_CIDADE_LENGTH, "city_too_long", "cidade");

        return new FamilyData(
                normalizedName, normalizedCpf, members, normalizedPhone, normalizedStreet,
                normalizedNumber, normalizedDistrict, normalizedCity
        );
    }

    private static long parseId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new ValidationException("invalid_id", "Informe uma família válida.");
        }
        try {
            long id = Long.parseLong(rawId.trim());
            validatePositiveId(id);
            return id;
        } catch (NumberFormatException exception) {
            throw new ValidationException("invalid_id", "Informe uma família válida.");
        }
    }

    private static void validatePositiveId(long id) {
        if (id <= 0) {
            throw new ValidationException("invalid_id", "Informe uma família válida.");
        }
    }

    private static String required(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(code, message);
        }
        return value.trim();
    }

    private static String optional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void validateLength(String value, int maximum, String code, String field) {
        if (value != null && value.length() > maximum) {
            throw new ValidationException(
                    code, "O campo " + field + " deve possuir no máximo " + maximum + " caracteres."
            );
        }
    }

    private record FamilyData(
            String nome,
            String cpf,
            int quantidadeMembros,
            String telefone,
            String logradouro,
            String numeroResidencia,
            String bairro,
            String cidade
    ) { }
}
