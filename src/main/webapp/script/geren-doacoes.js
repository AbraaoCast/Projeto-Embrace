let pendingDonationDeletionId = null;

const donationMessages = {
    created: 'Doação cadastrada com sucesso.',
    updated: 'Doação atualizada com sucesso.',
    donation_not_found: 'Doação não encontrada.',
    donation_has_related_records: 'Não é possível excluir esta doação porque existem entregas vinculadas a ela.',
    invalid_id: 'A doação informada é inválida.',
    internal: 'Não foi possível concluir a operação. Tente novamente.'
};

async function carregarDoacoes() {
    const tbody = document.getElementById('tbody-doacoes');
    tbody.replaceChildren();

    try {
        const response = await fetch('api/doacoes', { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);

        const doacoes = await response.json();
        if (doacoes.length === 0) {
            const row = tbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 6;
            cell.textContent = 'Nenhuma doação cadastrada.';
            return;
        }

        doacoes.forEach(doacao => tbody.appendChild(criarLinhaDoacao(doacao)));
    } catch (error) {
        const row = tbody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 6;
        cell.textContent = 'Erro ao carregar as doações.';
        console.error(error);
    }
}

function criarLinhaDoacao(doacao) {
    const row = document.createElement('tr');
    row.dataset.id = doacao.id;

    adicionarCelulaDoacao(row, doacao.tipo);
    adicionarCelulaDoacao(row, doacao.descricao);
    adicionarCelulaDoacao(row, String(doacao.quantidade));
    adicionarCelulaDoacao(row, formatarDataDoacao(doacao.dataDoacao));
    adicionarCelulaDoacao(row, doacao.observacao || '—');

    const actionCell = row.insertCell();
    const actions = document.createElement('div');
    actions.className = 'row-actions';

    const editButton = document.createElement('button');
    editButton.type = 'button';
    editButton.className = 'btn-editar btn-sm';
    editButton.textContent = 'Editar';
    editButton.addEventListener('click', () => editarDoacao(doacao.id));

    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'btn-excluir btn-sm';
    deleteButton.textContent = 'Excluir';
    deleteButton.addEventListener('click', () => confirmarExclusaoDoacao(doacao.id));

    actions.append(editButton, deleteButton);
    actionCell.appendChild(actions);
    return row;
}

function adicionarCelulaDoacao(row, value) {
    const cell = row.insertCell();
    cell.textContent = value;
}

function formatarDataDoacao(value) {
    const [year, month, day] = value.split('-');
    return year && month && day ? `${day}/${month}/${year}` : value;
}

function editarDoacao(id) {
    window.location.href = `editar-doacao.html?id=${encodeURIComponent(id)}`;
}

function confirmarExclusaoDoacao(id) {
    pendingDonationDeletionId = id;
    openPopup('popup-excluir');
}

async function performDelete() {
    if (!pendingDonationDeletionId) return;

    const id = pendingDonationDeletionId;
    pendingDonationDeletionId = null;

    try {
        const response = await fetch('doacoes/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: new URLSearchParams({ id: String(id) })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || donationMessages.internal);
        }

        closePopup('popup-excluir');
        mostrarMensagemDoacao('Doação excluída com sucesso.', 'success');
        await carregarDoacoes();
    } catch (error) {
        closePopup('popup-excluir');
        mostrarMensagemDoacao(error.message || donationMessages.internal, 'error');
    }
}

function filtrarDoacoes(term) {
    const normalizedTerm = term.trim().toLocaleLowerCase('pt-BR');
    document.querySelectorAll('#tbody-doacoes tr[data-id]').forEach(row => {
        row.hidden = !row.textContent.toLocaleLowerCase('pt-BR').includes(normalizedTerm);
    });
}

function mostrarMensagemDoacao(message, type) {
    const element = document.getElementById('donation-message');
    element.textContent = message;
    element.className = `page-message ${type}`;
}

function mostrarMensagemDoacaoDaUrl() {
    const query = new URLSearchParams(window.location.search);
    const success = query.get('success');
    const error = query.get('error');

    if (success && donationMessages[success]) {
        mostrarMensagemDoacao(donationMessages[success], 'success');
    } else if (error && donationMessages[error]) {
        mostrarMensagemDoacao(donationMessages[error], 'error');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    mostrarMensagemDoacaoDaUrl();
    document.getElementById('donation-search').addEventListener('input', event => {
        filtrarDoacoes(event.target.value);
    });
    carregarDoacoes();
});
