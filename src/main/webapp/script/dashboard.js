function renderDashboard(data) {
    document.getElementById('dashboard-total-families').textContent = data.resumo.totalFamilias;
    document.getElementById('dashboard-total-donations').textContent = data.resumo.totalDoacoes;
    document.getElementById('dashboard-total-deliveries').textContent = data.resumo.totalEntregas;
    document.getElementById('dashboard-attended-families').textContent = data.resumo.familiasAtendidas;

    const list = document.getElementById('dashboard-recent-activities');
    list.replaceChildren();
    if (data.atividadesRecentes.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'item-ativ empty-activity';
        empty.textContent = 'Nenhuma atividade registrada.';
        list.appendChild(empty);
        return;
    }

    data.atividadesRecentes.forEach(activity => list.appendChild(createActivity(activity)));
}

function createActivity(activity) {
    const item = document.createElement('div');
    item.className = 'item-ativ';

    const dot = document.createElement('div');
    dot.className = `dot ${activity.tipo}`;

    const text = document.createElement('div');
    text.className = 'text-ativ';
    text.textContent = activityDescription(activity);

    const date = document.createElement('div');
    date.className = 'time-ativ';
    date.textContent = activity.data;

    item.append(dot, text, date);
    return item;
}

function activityDescription(activity) {
    if (activity.tipo === 'familia') {
        return `Nova família cadastrada — ${activity.titulo}`;
    }
    if (activity.tipo === 'doacao') {
        const donation = activity.detalhe
            ? `${activity.titulo} — ${activity.detalhe}`
            : activity.titulo;
        return `Nova doação registrada — ${donation} (${activity.quantidade})`;
    }
    return `Entrega realizada — ${activity.titulo} recebeu ${activity.quantidade} de ${activity.detalhe}`;
}

function showDashboardError(message) {
    const element = document.getElementById('dashboard-message');
    element.textContent = message;
    element.className = 'dashboard-message error';

    const list = document.getElementById('dashboard-recent-activities');
    list.replaceChildren();
    const empty = document.createElement('div');
    empty.className = 'item-ativ empty-activity';
    empty.textContent = 'Não foi possível carregar as atividades.';
    list.appendChild(empty);
}

async function loadDashboard() {
    try {
        const response = await fetch('api/dashboard', { headers: { Accept: 'application/json' } });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || 'Não foi possível carregar o Dashboard.');
        }
        renderDashboard(await response.json());
    } catch (error) {
        showDashboardError(error.message || 'Não foi possível carregar o Dashboard.');
    }
}

document.addEventListener('DOMContentLoaded', loadDashboard);
