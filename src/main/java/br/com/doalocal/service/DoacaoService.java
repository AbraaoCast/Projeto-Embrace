package br.com.doalocal.service;

import br.com.doalocal.dao.DoacaoDao;
import br.com.doalocal.exception.DoacaoNotFoundException;
import br.com.doalocal.exception.DonationQuantityBelowDeliveredException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.Doacao;
import br.com.doalocal.model.DoacaoEstoque;
import br.com.doalocal.util.DateFormats;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Valida e coordena as regras do gerenciamento de doações recebidas. */
public final class DoacaoService {

    public static final int MAX_TIPO_LENGTH = 50;
    public static final int MAX_DESCRICAO_LENGTH = 200;
    public static final int MAX_QUANTIDADE = 1_000_000;
    public static final int MAX_OBSERVACAO_LENGTH = 1_000;

    private final DoacaoDao doacaoDao;

    public DoacaoService() {
        this(new DoacaoDao());
    }

    public DoacaoService(DoacaoDao doacaoDao) {
        this.doacaoDao = doacaoDao;
    }

    public Doacao create(
            String tipo,
            String descricao,
            String quantidade,
            String dataDoacao,
            String observacao
    ) {
        DonationData data = validateAndNormalize(tipo, descricao, quantidade, dataDoacao, observacao);
        return doacaoDao.insert(
                data.tipo(), data.descricao(), data.quantidade(), data.dataDoacao(), data.observacao()
        );
    }

    public List<Doacao> listAll() {
        return doacaoDao.findAll();
    }

    public List<DoacaoEstoque> listStock() {
        return doacaoDao.findAllStock();
    }

    public Doacao findById(String rawId) {
        return findById(parseId(rawId));
    }

    public Doacao findById(long id) {
        validatePositiveId(id);
        return doacaoDao.findById(id).orElseThrow(DoacaoNotFoundException::new);
    }

    public Doacao update(
            String rawId,
            String tipo,
            String descricao,
            String quantidade,
            String dataDoacao,
            String observacao
    ) {
        long id = parseId(rawId);
        findById(id);
        DonationData data = validateAndNormalize(tipo, descricao, quantidade, dataDoacao, observacao);

        try {
            if (!doacaoDao.update(
                    id, data.tipo(), data.descricao(), data.quantidade(), data.dataDoacao(), data.observacao()
            )) {
                throw new DoacaoNotFoundException();
            }
        } catch (DonationQuantityBelowDeliveredException exception) {
            throw new ValidationException(
                    "quantity_below_delivered",
                    "A quantidade original não pode ser menor que as "
                            + exception.getDeliveredQuantity() + " unidades já entregues."
            );
        }
        return doacaoDao.findById(id).orElseThrow(DoacaoNotFoundException::new);
    }

    public void delete(String rawId) {
        long id = parseId(rawId);
        findById(id);
        if (!doacaoDao.delete(id)) {
            throw new DoacaoNotFoundException();
        }
    }

    private static DonationData validateAndNormalize(
            String tipo,
            String descricao,
            String quantidade,
            String dataDoacao,
            String observacao
    ) {
        String normalizedType = required(tipo, "type_required", "Informe o tipo da doação.");
        String normalizedDescription = required(
                descricao, "description_required", "Informe a descrição da doação."
        );
        String normalizedQuantity = required(
                quantidade, "quantity_required", "Informe a quantidade da doação."
        );
        String normalizedDate = required(dataDoacao, "date_required", "Informe a data da doação.");
        String normalizedObservation = optional(observacao);

        validateLength(normalizedType, MAX_TIPO_LENGTH, "type_too_long", "tipo");
        validateLength(
                normalizedDescription, MAX_DESCRICAO_LENGTH, "description_too_long", "descrição"
        );
        validateLength(
                normalizedObservation, MAX_OBSERVACAO_LENGTH, "observation_too_long", "observação"
        );

        int parsedQuantity;
        try {
            parsedQuantity = Integer.parseInt(normalizedQuantity);
        } catch (NumberFormatException exception) {
            throw new ValidationException("invalid_quantity", "Informe uma quantidade válida.");
        }
        if (parsedQuantity <= 0) {
            throw new ValidationException(
                    "quantity_must_be_positive", "A quantidade deve ser maior que zero."
            );
        }
        if (parsedQuantity > MAX_QUANTIDADE) {
            throw new ValidationException(
                    "quantity_too_large", "A quantidade deve ser no máximo " + MAX_QUANTIDADE + "."
            );
        }

        LocalDate parsedDate;
        try {
            parsedDate = DateFormats.parseUserDate(normalizedDate);
        } catch (DateTimeParseException exception) {
            throw new ValidationException(
                    "invalid_date", "Informe uma data válida no formato DD/MM/AAAA."
            );
        }

        return new DonationData(
                normalizedType, normalizedDescription, parsedQuantity, parsedDate, normalizedObservation
        );
    }

    private static long parseId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new ValidationException("invalid_id", "Informe uma doação válida.");
        }
        try {
            long id = Long.parseLong(rawId.trim());
            validatePositiveId(id);
            return id;
        } catch (NumberFormatException exception) {
            throw new ValidationException("invalid_id", "Informe uma doação válida.");
        }
    }

    private static void validatePositiveId(long id) {
        if (id <= 0) {
            throw new ValidationException("invalid_id", "Informe uma doação válida.");
        }
    }

    private static String required(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(code, message);
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validateLength(String value, int maximum, String code, String field) {
        if (value != null && value.length() > maximum) {
            throw new ValidationException(
                    code, "O campo " + field + " deve possuir no máximo " + maximum + " caracteres."
            );
        }
    }

    private record DonationData(
            String tipo,
            String descricao,
            int quantidade,
            LocalDate dataDoacao,
            String observacao
    ) { }
}
