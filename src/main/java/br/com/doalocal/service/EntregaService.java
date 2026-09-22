package br.com.doalocal.service;

import br.com.doalocal.dao.DoacaoDao;
import br.com.doalocal.dao.EntregaDao;
import br.com.doalocal.dao.FamiliaDao;
import br.com.doalocal.exception.DoacaoNotFoundException;
import br.com.doalocal.exception.FamiliaNotFoundException;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.DoacaoDisponivel;
import br.com.doalocal.model.Entrega;
import br.com.doalocal.model.EntregaResumo;
import br.com.doalocal.util.DateFormats;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Regras de registro, disponibilidade e consulta do histórico de entregas. */
public final class EntregaService {

    public static final int MAX_QUANTIDADE = DoacaoService.MAX_QUANTIDADE;
    public static final int MAX_OBSERVACAO_LENGTH = 1_000;

    private final EntregaDao entregaDao;
    private final FamiliaDao familiaDao;
    private final DoacaoDao doacaoDao;

    public EntregaService() {
        this(new EntregaDao(), new FamiliaDao(), new DoacaoDao());
    }

    public EntregaService(
            EntregaDao entregaDao,
            FamiliaDao familiaDao,
            DoacaoDao doacaoDao
    ) {
        this.entregaDao = entregaDao;
        this.familiaDao = familiaDao;
        this.doacaoDao = doacaoDao;
    }

    public Entrega register(
            String familiaId,
            String doacaoId,
            String quantidadeEntregue,
            String dataEntrega,
            String observacao
    ) {
        long parsedFamilyId = parseId(
                familiaId, "family_required", "Selecione uma família."
        );
        long parsedDonationId = parseId(
                doacaoId, "donation_required", "Selecione uma doação."
        );
        int parsedQuantity = parseQuantity(quantidadeEntregue);
        LocalDate parsedDate = parseDate(dataEntrega);
        String normalizedObservation = normalizeObservation(observacao);

        if (familiaDao.findById(parsedFamilyId).isEmpty()) {
            throw new FamiliaNotFoundException();
        }
        if (doacaoDao.findById(parsedDonationId).isEmpty()) {
            throw new DoacaoNotFoundException();
        }

        return entregaDao.insertWithAvailabilityCheck(
                parsedFamilyId,
                parsedDonationId,
                parsedQuantity,
                parsedDate,
                normalizedObservation
        );
    }

    public List<EntregaResumo> listHistory() {
        return entregaDao.findAllSummaries();
    }

    public List<DoacaoDisponivel> listDonationAvailability() {
        return entregaDao.findDonationAvailability();
    }

    private static long parseId(String value, String missingCode, String missingMessage) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(missingCode, missingMessage);
        }
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) {
                throw new ValidationException("invalid_id", "O identificador informado é inválido.");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new ValidationException("invalid_id", "O identificador informado é inválido.");
        }
    }

    private static int parseQuantity(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("quantity_required", "Informe a quantidade entregue.");
        }
        int quantity;
        try {
            quantity = Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new ValidationException("invalid_quantity", "Informe uma quantidade válida.");
        }
        if (quantity <= 0) {
            throw new ValidationException(
                    "quantity_must_be_positive", "A quantidade deve ser maior que zero."
            );
        }
        if (quantity > MAX_QUANTIDADE) {
            throw new ValidationException(
                    "quantity_too_large", "A quantidade deve ser no máximo " + MAX_QUANTIDADE + "."
            );
        }
        return quantity;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("date_required", "Informe a data da entrega.");
        }
        try {
            return DateFormats.parseUserDate(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ValidationException(
                    "invalid_date", "Informe uma data válida no formato DD/MM/AAAA."
            );
        }
    }

    private static String normalizeObservation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_OBSERVACAO_LENGTH) {
            throw new ValidationException(
                    "observation_too_long",
                    "A observação deve possuir no máximo " + MAX_OBSERVACAO_LENGTH + " caracteres."
            );
        }
        return normalized;
    }
}
