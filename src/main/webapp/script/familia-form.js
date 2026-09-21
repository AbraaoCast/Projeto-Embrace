const familyFormMessages = {
    name_required: 'Informe o nome do responsável.',
    cpf_required: 'Informe o CPF do responsável.',
    invalid_cpf: 'Informe um CPF válido.',
    duplicate_cpf: 'Já existe uma família cadastrada com este CPF.',
    members_required: 'Informe a quantidade de membros.',
    invalid_members: 'Informe uma quantidade de membros válida.',
    members_must_be_positive: 'A quantidade de membros deve ser maior que zero.',
    members_too_large: 'A quantidade de membros deve ser no máximo 100.',
    phone_required: 'Informe o telefone.',
    address_required: 'Informe o endereço.',
    district_required: 'Informe o bairro.',
    city_required: 'Informe a cidade.',
    name_too_long: 'O nome do responsável é muito longo.',
    phone_too_long: 'O telefone é muito longo.',
    address_too_long: 'O endereço é muito longo.',
    number_too_long: 'O número da residência é muito longo.',
    district_too_long: 'O bairro é muito longo.',
    city_too_long: 'A cidade é muito longa.',
    invalid_id: 'A família informada é inválida.',
    family_not_found: 'Família não encontrada.',
    internal: 'Não foi possível concluir a operação. Tente novamente.'
};

function showFamilyFormMessage(message, type = 'error') {
    const element = document.getElementById('family-message');
    if (!element) return;
    element.textContent = message;
    element.className = `page-message ${type}`;
}

function formatFamilyCpf(cpf) {
    return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
}

function formatFamilyDate(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('pt-BR');
}

async function loadFamilyForEditing() {
    const form = document.getElementById('edit-family-form');
    if (!form) return;

    const id = new URLSearchParams(window.location.search).get('id');
    if (!id || !/^\d+$/.test(id) || Number(id) <= 0) {
        showFamilyFormMessage(familyFormMessages.invalid_id);
        form.querySelectorAll('input, button').forEach(element => element.disabled = true);
        return;
    }

    try {
        const response = await fetch(`api/familias/${encodeURIComponent(id)}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || familyFormMessages.internal);
        }

        const familia = await response.json();
        document.getElementById('id').value = familia.id;
        document.getElementById('nomeResponsavel').value = familia.nomeResponsavel;
        document.getElementById('cpf').value = formatFamilyCpf(familia.cpf);
        document.getElementById('quantidadeMembros').value = familia.quantidadeMembros;
        document.getElementById('telefone').value = familia.telefone;
        document.getElementById('logradouro').value = familia.logradouro;
        document.getElementById('numeroResidencia').value = familia.numeroResidencia || '';
        document.getElementById('bairro').value = familia.bairro;
        document.getElementById('cidade').value = familia.cidade;
        document.getElementById('dataCadastro').value = formatFamilyDate(familia.dataCadastro);
    } catch (error) {
        showFamilyFormMessage(error.message || familyFormMessages.internal);
        form.querySelectorAll('input, button').forEach(element => element.disabled = true);
    }
}

function showFamilyQueryMessage() {
    const error = new URLSearchParams(window.location.search).get('error');
    if (error) showFamilyFormMessage(familyFormMessages[error] || familyFormMessages.internal);
}

function enableCpfMask() {
    const input = document.getElementById('cpf');
    if (!input) return;
    input.addEventListener('input', () => {
        const digits = input.value.replace(/\D/g, '').slice(0, 11);
        input.value = digits
            .replace(/^(\d{3})(\d)/, '$1.$2')
            .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
            .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
    });
}

document.addEventListener('DOMContentLoaded', () => {
    showFamilyQueryMessage();
    enableCpfMask();
    loadFamilyForEditing();
});
