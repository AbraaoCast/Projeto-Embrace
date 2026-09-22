let pendingFamilyDeletionId = null;

const familyMessages = {
    created: 'Família cadastrada com sucesso.',
    updated: 'Dados da família atualizados com sucesso.',
    family_not_found: 'Família não encontrada.',
    family_has_related_records: 'Não é possível excluir esta família porque existem registros vinculados a ela.',
    invalid_id: 'A família informada é inválida.',
    internal: 'Não foi possível concluir a operação. Tente novamente.'
};

async function carregarFamilias() {
    const tbody = document.getElementById('tbody-familias');
    tbody.replaceChildren();

    try {
        const response = await fetch('api/familias', { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);

        const familias = await response.json();
        if (familias.length === 0) {
            const row = tbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 7;
            cell.textContent = 'Nenhuma família cadastrada.';
            return;
        }

        familias.forEach(familia => tbody.appendChild(criarLinhaFamilia(familia)));
    } catch (error) {
        const row = tbody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 7;
        cell.textContent = 'Erro ao carregar as famílias.';
        console.error(error);
    }
}

function criarLinhaFamilia(familia) {
    const row = document.createElement('tr');
    row.dataset.id = familia.id;

    adicionarCelula(row, familia.nomeResponsavel);
    adicionarCelula(row, formatarCpf(familia.cpf));
    adicionarCelula(row, String(familia.quantidadeMembros));
    adicionarCelula(row, DoaLocalFormats.formatPhone(familia.telefone));
    adicionarCelula(row, formatarEndereco(familia));
    adicionarCelula(row, DoaLocalFormats.formatDate(familia.dataCadastro));

    const actionCell = row.insertCell();
    const actions = document.createElement('div');
    actions.className = 'row-actions';

    const editButton = document.createElement('button');
    editButton.type = 'button';
    editButton.className = 'btn-editar btn-sm';
    editButton.textContent = 'Editar';
    editButton.addEventListener('click', () => editarFamilia(familia.id));

    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'btn-excluir btn-sm';
    deleteButton.textContent = 'Excluir';
    deleteButton.addEventListener('click', () => confirmarExclusaoFamilia(familia.id));

    actions.append(editButton, deleteButton);
    actionCell.appendChild(actions);
    return row;
}

function adicionarCelula(row, value) {
    const cell = row.insertCell();
    cell.textContent = value;
}

function formatarEndereco(familia) {
    const numero = familia.numeroResidencia ? `, ${familia.numeroResidencia}` : '';
    return `${familia.logradouro}${numero} — ${familia.bairro}, ${familia.cidade}`;
}

function formatarCpf(cpf) {
    return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
}

function editarFamilia(id) {
    window.location.href = `editar-fami.html?id=${encodeURIComponent(id)}`;
}

function confirmarExclusaoFamilia(id) {
    pendingFamilyDeletionId = id;
    openPopup('popup-excluir');
}

async function performDelete() {
    if (!pendingFamilyDeletionId) return;

    const id = pendingFamilyDeletionId;
    pendingFamilyDeletionId = null;

    try {
        const response = await fetch('familias/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: new URLSearchParams({ id: String(id) })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || familyMessages.internal);
        }

        closePopup('popup-excluir');
        mostrarMensagem('Família excluída com sucesso.', 'success');
        await carregarFamilias();
    } catch (error) {
        closePopup('popup-excluir');
        mostrarMensagem(error.message || familyMessages.internal, 'error');
    }
}

function filtrarFamilias(term) {
    const normalizedTerm = term.trim().toLocaleLowerCase('pt-BR');
    document.querySelectorAll('#tbody-familias tr[data-id]').forEach(row => {
        row.hidden = !row.textContent.toLocaleLowerCase('pt-BR').includes(normalizedTerm);
    });
}

function mostrarMensagem(message, type) {
    const element = document.getElementById('family-message');
    element.textContent = message;
    element.className = `page-message ${type}`;
}

function mostrarMensagemDaUrl() {
    const query = new URLSearchParams(window.location.search);
    const success = query.get('success');
    const error = query.get('error');

    if (success && familyMessages[success]) {
        mostrarMensagem(familyMessages[success], 'success');
    } else if (error && familyMessages[error]) {
        mostrarMensagem(familyMessages[error], 'error');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    mostrarMensagemDaUrl();
    document.getElementById('family-search').addEventListener('input', event => {
        filtrarFamilias(event.target.value);
    });
    carregarFamilias();
});
