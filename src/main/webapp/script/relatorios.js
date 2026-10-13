const reportMessages = {
    invalid_start_date: 'Informe uma data inicial válida no formato DD/MM/AAAA.',
    invalid_end_date: 'Informe uma data final válida no formato DD/MM/AAAA.',
    invalid_period: 'A data inicial não pode ser posterior à data final.',
    invalid_family: 'Selecione uma família válida.',
    invalid_donation: 'Selecione uma doação válida.',
    family_not_found: 'Família não encontrada.',
    donation_not_found: 'Doação não encontrada.',
    internal: 'Não foi possível gerar o relatório. Tente novamente.'
};

function reportParameters() {
    const parameters = new URLSearchParams();
    const values = {
        dataInicial: document.getElementById('report-start-date').value.trim(),
        dataFinal: document.getElementById('report-end-date').value.trim(),
        familiaId: document.getElementById('report-family').value,
        doacaoId: document.getElementById('report-donation').value
    };
    Object.entries(values).forEach(([name, value]) => {
        if (value) parameters.set(name, value);
    });
    return parameters;
}

async function loadReportOptions() {
    const response = await fetch('api/relatorios/opcoes', { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error(reportMessages.internal);
    const options = await response.json();

    const familySelect = document.getElementById('report-family');
    familySelect.replaceChildren(reportOption('', 'Todas as famílias'));
    options.familias.forEach(family => {
        familySelect.appendChild(reportOption(String(family.id), family.nomeResponsavel));
    });

    const donationSelect = document.getElementById('report-donation');
    donationSelect.replaceChildren(reportOption('', 'Todas as doações'));
    options.doacoes.forEach(donation => {
        donationSelect.appendChild(reportOption(
            String(donation.id), `${donation.tipo} — ${donation.descricao}`
        ));
    });
}

function reportOption(value, label) {
    const option = document.createElement('option');
    option.value = value;
    option.textContent = label;
    return option;
}

async function generateReport(showSuccess = true) {
    const startDate = document.getElementById('report-start-date').value.trim();
    const endDate = document.getElementById('report-end-date').value.trim();
    if (startDate && !DoaLocalFormats.isValidDate(startDate)) {
        showReportMessage(reportMessages.invalid_start_date, 'error');
        document.getElementById('report-start-date').focus();
        return false;
    }
    if (endDate && !DoaLocalFormats.isValidDate(endDate)) {
        showReportMessage(reportMessages.invalid_end_date, 'error');
        document.getElementById('report-end-date').focus();
        return false;
    }

    try {
        const response = await fetch(`api/relatorios?${reportParameters()}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(reportMessages[error.error] || error.message || reportMessages.internal);
        }
        const report = await response.json();
        renderReport(report);
        if (showSuccess) showReportMessage('Relatório gerado com sucesso.', 'success');
        return true;
    } catch (error) {
        showReportMessage(error.message || reportMessages.internal, 'error');
        return false;
    }
}

function renderReport(report) {
    document.getElementById('total-families').textContent = report.resumo.totalFamiliasCadastradas;
    document.getElementById('total-donations').textContent = report.resumo.totalDoacoesCadastradas;
    document.getElementById('total-deliveries').textContent = report.resumo.totalEntregas;
    document.getElementById('attended-families').textContent = report.resumo.familiasAtendidas;

    const body = document.getElementById('tbody-relatorios');
    body.replaceChildren();
    if (report.itens.length === 0) {
        const row = body.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 6;
        cell.textContent = 'Nenhuma entrega encontrada para os filtros informados.';
        return;
    }

    report.itens.forEach(item => {
        const row = body.insertRow();
        addReportCell(row, item.nomeResponsavel);
        addReportCell(row, item.cpfResponsavel);
        addReportCell(row, String(item.quantidadeMembros));
        addReportCell(row, `${item.tipoDoacao} — ${item.descricaoDoacao}`);
        addReportCell(row, String(item.quantidadeEntregue));
        addReportCell(row, item.dataEntrega);
    });
}

function addReportCell(row, value) {
    const cell = row.insertCell();
    cell.textContent = value;
}

function showReportMessage(message, type) {
    const element = document.getElementById('report-message');
    element.textContent = message;
    element.className = `page-message ${type}`;
}

async function downloadReport() {
    if (!await generateReport(false)) return;
    window.location.href = `relatorios/pdf?${reportParameters()}`;
}

document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('generate-report').addEventListener('click', () => generateReport());
    document.getElementById('download-report').addEventListener('click', downloadReport);
    try {
        await loadReportOptions();
        await generateReport(false);
    } catch (error) {
        showReportMessage(error.message || reportMessages.internal, 'error');
    }
});
