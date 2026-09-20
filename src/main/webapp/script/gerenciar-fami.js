async function carregarFamilias() {

    const tbody = document.getElementById('tbody-familias');

    try {
        const resposta = await fetch('http://localhost:8080/api/familias');
        const familias = await resposta.json();

        tbody.innerHTML = '';

        if (familias.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7">Nenhuma família cadastrada.</td></tr>';
            return;
        }

        familias.forEach(familia => {

            const tr = document.createElement('tr');
            tr.dataset.id = familia.id;   // guarda o id para editar/excluir

            tr.innerHTML = `
                <td>${familia.titular}</td>
                <td>${familia.cpf}</td>
                <td>${familia.quantidadeMembros}</td>
                <td>${familia.telefone}</td>
                <td>${familia.endereco}, ${familia.numero} — ${familia.bairro}, ${familia.cidade}</td>
                <td>${familia.dataCadastro}</td>
                <td>
                    <div class="row-actions">
                        <button class="btn btn-edit btn-sm" onclick="editarFamilia(${familia.id})">Editar</button>
                        <button class="btn btn-delete btn-sm" onclick="confirmarExclusao(${familia.id})">Excluir</button>
                    </div>
                </td>
            `;

            tbody.appendChild(tr);
        });

    } catch (erro) {
        tbody.innerHTML = '<tr><td colspan="7">Erro ao carregar as famílias.</td></tr>';
        console.error(erro);
    }
}

document.addEventListener('DOMContentLoaded', carregarFamilias);