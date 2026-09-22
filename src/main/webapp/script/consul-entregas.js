let deliveryHistory = [];

async function carregarEntregas() {
    const tbody = document.getElementById('tbody-entregas');
    tbody.replaceChildren();

    try {
        const response = await fetch('api/entregas', { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);

        deliveryHistory = await response.json();
        populateDeliveryFilters(deliveryHistory);
        renderDeliveries();
    } catch (error) {
        showEmptyDeliveryRow('Erro ao carregar as entregas.');
        console.error(error);
    }
}

function renderDeliveries() {
    const tbody = document.getElementById('tbody-entregas');
    const search = document.getElementById('delivery-search').value.trim().toLocaleLowerCase('pt-BR');
    const familyId = document.getElementById('filter-family').value;
    const date = document.getElementById('filter-date').value;
    const type = document.getElementById('filter-type').value;

    const filtered = deliveryHistory.filter(delivery => {
        const searchable = [
            delivery.nomeResponsavel,
            delivery.cpfResponsavel,
            delivery.tipoDoacao,
            delivery.descricaoDoacao,
            delivery.observacao || ''
        ].join(' ').toLocaleLowerCase('pt-BR');

        return (!search || searchable.includes(search))
            && (!familyId || String(delivery.familiaId) === familyId)
            && (!date || delivery.dataEntrega === date)
            && (!type || delivery.tipoDoacao === type);
    });

    tbody.replaceChildren();
    if (filtered.length === 0) {
        showEmptyDeliveryRow(deliveryHistory.length === 0
            ? 'Nenhuma entrega registrada.'
            : 'Nenhuma entrega encontrada para os filtros informados.');
        return;
    }

    filtered.forEach(delivery => tbody.appendChild(createDeliveryRow(delivery)));
}

function createDeliveryRow(delivery) {
    const row = document.createElement('tr');
    row.dataset.id = delivery.id;

    addDeliveryCell(row, delivery.nomeResponsavel);
    addDeliveryCell(row, `${delivery.tipoDoacao} — ${delivery.descricaoDoacao}`);
    addDeliveryCell(row, String(delivery.quantidadeEntregue));
    addDeliveryCell(row, formatDeliveryDate(delivery.dataEntrega));
    addDeliveryCell(row, delivery.observacao || '—');
    addDeliveryCell(row, '—');
    return row;
}

function addDeliveryCell(row, value) {
    const cell = row.insertCell();
    cell.textContent = value;
}

function showEmptyDeliveryRow(message) {
    const tbody = document.getElementById('tbody-entregas');
    tbody.replaceChildren();
    const row = tbody.insertRow();
    const cell = row.insertCell();
    cell.colSpan = 6;
    cell.textContent = message;
}

function populateDeliveryFilters(deliveries) {
    const families = new Map();
    const types = new Set();
    deliveries.forEach(delivery => {
        families.set(String(delivery.familiaId), delivery.nomeResponsavel);
        types.add(delivery.tipoDoacao);
    });

    const familySelect = document.getElementById('filter-family');
    familySelect.replaceChildren(createDeliveryOption('', 'Todas as famílias'));
    [...families.entries()]
        .sort((first, second) => first[1].localeCompare(second[1], 'pt-BR'))
        .forEach(([id, name]) => familySelect.appendChild(createDeliveryOption(id, name)));

    const typeSelect = document.getElementById('filter-type');
    typeSelect.replaceChildren(createDeliveryOption('', 'Todos os tipos'));
    [...types].sort((first, second) => first.localeCompare(second, 'pt-BR'))
        .forEach(type => typeSelect.appendChild(createDeliveryOption(type, type)));
}

function createDeliveryOption(value, label) {
    const option = document.createElement('option');
    option.value = value;
    option.textContent = label;
    return option;
}

function formatDeliveryDate(value) {
    const [year, month, day] = value.split('-');
    return year && month && day ? `${day}/${month}/${year}` : value;
}

function showDeliverySuccessMessage() {
    const success = new URLSearchParams(window.location.search).get('success');
    if (success !== 'created') return;
    const element = document.getElementById('delivery-message');
    element.textContent = 'Entrega registrada com sucesso.';
    element.className = 'page-message success';
}

document.addEventListener('DOMContentLoaded', () => {
    showDeliverySuccessMessage();
    ['delivery-search', 'filter-family', 'filter-date', 'filter-type'].forEach(id => {
        document.getElementById(id).addEventListener('input', renderDeliveries);
    });
    carregarEntregas();
});
