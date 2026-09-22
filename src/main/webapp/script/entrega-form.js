const deliveryFormMessages = {
    family_required: 'Selecione uma família.',
    donation_required: 'Selecione uma doação.',
    invalid_id: 'A família ou doação informada é inválida.',
    family_not_found: 'Família não encontrada.',
    donation_not_found: 'Doação não encontrada.',
    quantity_required: 'Informe a quantidade entregue.',
    invalid_quantity: 'Informe uma quantidade válida.',
    quantity_must_be_positive: 'A quantidade deve ser maior que zero.',
    quantity_too_large: 'A quantidade informada é muito grande.',
    insufficient_quantity: 'A quantidade informada é maior que a quantidade disponível para esta doação.',
    date_required: 'Informe a data da entrega.',
    invalid_date: 'Informe uma data válida no formato DD/MM/AAAA.',
    observation_too_long: 'A observação deve possuir no máximo 1.000 caracteres.',
    internal: 'Não foi possível registrar a entrega. Tente novamente.'
};

function showDeliveryFormMessage(message) {
    const element = document.getElementById('delivery-message');
    element.textContent = message;
    element.className = 'page-message error';
}

function formatDeliveryCpf(cpf) {
    return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
}

async function loadDeliveryOptions() {
    const familySelect = document.getElementById('familiaId');
    const donationSelect = document.getElementById('doacaoId');
    const submitButton = document.getElementById('submit-delivery');

    try {
        const response = await fetch('api/entregas/opcoes', {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw new Error(deliveryFormMessages.internal);

        const options = await response.json();
        fillFamilyOptions(familySelect, options.familias);
        const availableDonations = options.doacoes.filter(donation => donation.quantidadeDisponivel > 0);
        fillDonationOptions(donationSelect, availableDonations);

        if (options.familias.length === 0) {
            showDeliveryFormMessage('Nenhuma família cadastrada. Cadastre uma família antes de registrar entregas.');
            submitButton.disabled = true;
        } else if (options.doacoes.length === 0) {
            showDeliveryFormMessage('Nenhuma doação cadastrada. Cadastre uma doação antes de registrar entregas.');
            submitButton.disabled = true;
        } else if (availableDonations.length === 0) {
            showDeliveryFormMessage('Nenhuma doação possui quantidade disponível para entrega.');
            submitButton.disabled = true;
        }
    } catch (error) {
        familySelect.replaceChildren(createOption('', 'Não foi possível carregar as famílias'));
        donationSelect.replaceChildren(createOption('', 'Não foi possível carregar as doações'));
        submitButton.disabled = true;
        showDeliveryFormMessage(error.message || deliveryFormMessages.internal);
    }
}

function fillFamilyOptions(select, families) {
    select.replaceChildren(createOption('', families.length ? 'Selecione a família' : 'Nenhuma família cadastrada'));
    families.forEach(family => {
        select.appendChild(createOption(
            String(family.id),
            `${family.nomeResponsavel} — ${formatDeliveryCpf(family.cpf)}`
        ));
    });
}

function fillDonationOptions(select, donations) {
    select.replaceChildren(createOption('', donations.length ? 'Selecione a doação' : 'Nenhuma doação disponível'));
    donations.forEach(donation => {
        select.appendChild(createOption(
            String(donation.id),
            `${donation.tipo} — ${donation.descricao} (disponível: ${donation.quantidadeDisponivel})`
        ));
    });
}

function createOption(value, label) {
    const option = document.createElement('option');
    option.value = value;
    option.textContent = label;
    return option;
}

function showDeliveryQueryError() {
    const error = new URLSearchParams(window.location.search).get('error');
    if (error) {
        showDeliveryFormMessage(deliveryFormMessages[error] || deliveryFormMessages.internal);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    showDeliveryQueryError();
    loadDeliveryOptions();
});
