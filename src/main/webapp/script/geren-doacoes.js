// Função para apresentar os dados das doações cadastradas na pag. Gerenciar doações
async function carregarDoacoes() {

    const tbody = document.getElementById('tbody-doacoes');

    try {
        const resposta = await fetch('http://localhost:8080/api/doacoes');
        if (!resposta.ok) throw new Error('Erro HTTP: ' + resposta.status);

        const doacoes = await resposta.json();
        tbody.innerHTML = '';

        if (doacoes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">Nenhuma doação cadastrada.</td></tr>';
            return;
        }

        doacoes.forEach(doacao => {
            const tr = document.createElement('tr');
            tr.dataset.id = doacao.id;

            tr.innerHTML = `
                <td>${doacao.tipo}</td>
                <td>${doacao.descricao}</td>
                <td>${doacao.quantidade}</td>
                <td>${doacao.dataDoacao}</td>
                <td>${doacao.observacao ?? '—'}</td>
                <td>
                    <div class="row-actions">
                        
                        <button class="btn-excluir btn-sm" onclick="confirmarExclusaoDoacao(${doacao.id})">Excluir</button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });

    } catch (erro) {
        tbody.innerHTML = '<tr><td colspan="6">Erro ao carregar as doações.</td></tr>';
        console.error(erro);
    }
}

function editarDoacao(id) { /* ... */ }
function confirmarExclusaoDoacao(id) { /* ... */ }

document.addEventListener('DOMContentLoaded', carregarDoacoes);