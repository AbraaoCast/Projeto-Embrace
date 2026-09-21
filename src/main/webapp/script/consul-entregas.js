// Função para apresentar os dados das entregas cadastradas na pag. Consultar entregas
async function carregarEntregas() {

    const tbody = document.getElementById('tbody-entregas');

    try {
        const resposta = await fetch('http://localhost:8080/api/entregas');
        if (!resposta.ok) throw new Error('Erro HTTP: ' + resposta.status);

        const entregas = await resposta.json();
        tbody.innerHTML = '';

        if (entregas.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">Nenhuma entrega registrada.</td></tr>';
            return;
        }

        entregas.forEach(entrega => {
            const tr = document.createElement('tr');
            tr.dataset.id = entrega.id;

            tr.innerHTML = `
                <td>${entrega.familia}</td>
                <td>${entrega.doacao}</td>
                <td>${entrega.quantidadeEntregue}</td>
                <td>${entrega.dataEntrega}</td>
                <td>${entrega.observacao ?? '—'}</td>
                <td>
                    <div class="row-actions">
                        <button class="btn-visualizar btn-sm" onclick="visualizarEntrega(${entrega.id})">Visualizar</button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });

    } catch (erro) {
        tbody.innerHTML = '<tr><td colspan="6">Erro ao carregar as entregas.</td></tr>';
        console.error(erro);
    }
}

function visualizarEntrega(id) { /* ... */ }

document.addEventListener('DOMContentLoaded', carregarEntregas);