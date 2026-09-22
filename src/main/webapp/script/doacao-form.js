const donationFormMessages = {
    type_required: 'Informe o tipo da doação.',
    type_too_long: 'O tipo da doação é muito longo.',
    description_required: 'Informe a descrição da doação.',
    description_too_long: 'A descrição da doação é muito longa.',
    quantity_required: 'Informe a quantidade da doação.',
    invalid_quantity: 'Informe uma quantidade válida.',
    quantity_must_be_positive: 'A quantidade deve ser maior que zero.',
    quantity_too_large: 'A quantidade deve ser no máximo 1.000.000.',
    date_required: 'Informe a data da doação.',
    invalid_date: 'Informe uma data válida.',
    observation_too_long: 'A observação deve possuir no máximo 1.000 caracteres.',
    invalid_id: 'A doação informada é inválida.',
    donation_not_found: 'Doação não encontrada.',
    internal: 'Não foi possível concluir a operação. Tente novamente.'
};

function showDonationFormMessage(message) {
    const element = document.getElementById('donation-message');
    if (!element) return;
    element.textContent = message;
    element.className = 'page-message error';
}

function showDonationQueryMessage() {
    const error = new URLSearchParams(window.location.search).get('error');
    if (error) {
        showDonationFormMessage(donationFormMessages[error] || donationFormMessages.internal);
    }
}

async function loadDonationForEditing() {
    const form = document.getElementById('edit-donation-form');
    if (!form) return;

    const id = new URLSearchParams(window.location.search).get('id');
    if (!id || !/^\d+$/.test(id) || Number(id) <= 0) {
        showDonationFormMessage(donationFormMessages.invalid_id);
        disableDonationForm(form);
        return;
    }

    try {
        const response = await fetch(`api/doacoes/${encodeURIComponent(id)}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || donationFormMessages.internal);
        }

        const doacao = await response.json();
        document.getElementById('id').value = doacao.id;
        document.getElementById('tipo').value = doacao.tipo;
        document.getElementById('descricao').value = doacao.descricao;
        document.getElementById('quantidade').value = doacao.quantidade;
        document.getElementById('dataDoacao').value = doacao.dataDoacao;
        document.getElementById('observacao').value = doacao.observacao || '';
    } catch (error) {
        showDonationFormMessage(error.message || donationFormMessages.internal);
        disableDonationForm(form);
    }
}

function disableDonationForm(form) {
    form.querySelectorAll('input, select, textarea, button').forEach(element => {
        element.disabled = true;
    });
}

document.addEventListener('DOMContentLoaded', () => {
    showDonationQueryMessage();
    loadDonationForEditing();
});
