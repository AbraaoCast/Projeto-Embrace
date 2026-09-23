package br.com.doalocal.service;

import br.com.doalocal.dao.RelatorioDao;
import br.com.doalocal.exception.ValidationException;
import br.com.doalocal.model.RelatorioEntregaItem;
import br.com.doalocal.model.RelatorioFiltro;
import br.com.doalocal.model.RelatorioOpcoes;
import br.com.doalocal.model.RelatorioQuantidade;
import br.com.doalocal.model.RelatorioResultado;
import br.com.doalocal.model.RelatorioResumo;
import br.com.doalocal.util.DateFormats;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Valida filtros e consolida os dados reais do relatório de Entregas. */
public final class RelatorioService {

    private final RelatorioDao relatorioDao;

    public RelatorioService() {
        this(new RelatorioDao());
    }

    public RelatorioService(RelatorioDao relatorioDao) {
        this.relatorioDao = relatorioDao;
    }

    public RelatorioResultado generate(
            String dataInicial,
            String dataFinal,
            String familiaId,
            String doacaoId
    ) {
        LocalDate start = parseOptionalDate(dataInicial, "invalid_start_date");
        LocalDate end = parseOptionalDate(dataFinal, "invalid_end_date");
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException(
                    "invalid_period", "A data inicial não pode ser posterior à data final."
            );
        }

        Long family = parseOptionalId(familiaId, "invalid_family", "Selecione uma família válida.");
        Long donation = parseOptionalId(doacaoId, "invalid_donation", "Selecione uma doação válida.");
        if (family != null && !relatorioDao.familyExists(family)) {
            throw new ValidationException("family_not_found", "Família não encontrada.");
        }
        if (donation != null && !relatorioDao.donationExists(donation)) {
            throw new ValidationException("donation_not_found", "Doação não encontrada.");
        }

        RelatorioFiltro filter = new RelatorioFiltro(start, end, family, donation);
        List<RelatorioEntregaItem> items = relatorioDao.findDeliveries(filter);
        int attendedFamilies = (int) items.stream()
                .map(RelatorioEntregaItem::familiaId)
                .distinct()
                .count();
        RelatorioResumo summary = new RelatorioResumo(
                relatorioDao.countFamilies(),
                relatorioDao.countDonations(),
                items.size(),
                attendedFamilies
        );
        return new RelatorioResultado(
                filter,
                summary,
                groupQuantities(items),
                List.copyOf(items)
        );
    }

    public RelatorioOpcoes listOptions() {
        return relatorioDao.findOptions();
    }

    private static List<RelatorioQuantidade> groupQuantities(List<RelatorioEntregaItem> items) {
        Map<Long, RelatorioQuantidade> grouped = new LinkedHashMap<>();
        for (RelatorioEntregaItem item : items) {
            String identification = item.tipoDoacao() + " - " + item.descricaoDoacao();
            RelatorioQuantidade current = grouped.get(item.doacaoId());
            int quantity = item.quantidadeEntregue()
                    + (current == null ? 0 : current.quantidade());
            grouped.put(item.doacaoId(), new RelatorioQuantidade(
                    item.doacaoId(), identification, quantity
            ));
        }
        return new ArrayList<>(grouped.values());
    }

    private static LocalDate parseOptionalDate(String value, String errorCode) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DateFormats.parseUserDate(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ValidationException(
                    errorCode, "Informe uma data válida no formato DD/MM/AAAA."
            );
        }
    }

    private static Long parseOptionalId(String value, String errorCode, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(value.trim());
            if (id <= 0) {
                throw new ValidationException(errorCode, message);
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new ValidationException(errorCode, message);
        }
    }
}
