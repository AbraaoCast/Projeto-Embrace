(function () {
    'use strict';

    function digitsOnly(value, maximum) {
        return String(value || '').replace(/\D/g, '').slice(0, maximum);
    }

    function normalizePhone(value) {
        return digitsOnly(value, 11);
    }

    function formatPhone(value) {
        const original = String(value || '').trim();
        const digits = original.replace(/\D/g, '');
        if (digits.length !== 11) return original;
        return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
    }

    function formatPhoneInput(value) {
        const digits = normalizePhone(value);
        if (!digits) return '';
        if (digits.length <= 2) return `(${digits}`;
        if (digits.length <= 7) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
        return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
    }

    function formatDateInput(value) {
        const digits = digitsOnly(value, 8);
        if (digits.length <= 2) return digits;
        if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
        return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
    }

    function formatDate(value) {
        const text = String(value || '').trim();
        const iso = /^(\d{4})-(\d{2})-(\d{2})/.exec(text);
        if (iso) return `${iso[3]}/${iso[2]}/${iso[1]}`;
        const brazilian = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(text);
        return brazilian ? text : text;
    }

    function dateToIso(value) {
        const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(String(value || '').trim());
        return match ? `${match[3]}-${match[2]}-${match[1]}` : '';
    }

    function applyMask(input, formatter) {
        input.value = formatter(input.value);
        input.addEventListener('input', () => {
            input.value = formatter(input.value);
        });
    }

    function applyMasks(root = document) {
        root.querySelectorAll('[data-mask="phone"]').forEach(input => applyMask(input, formatPhoneInput));
        root.querySelectorAll('[data-mask="date"]').forEach(input => applyMask(input, formatDateInput));
    }

    window.DoaLocalFormats = Object.freeze({
        normalizePhone,
        formatPhone,
        formatPhoneInput,
        formatDate,
        formatDateInput,
        dateToIso,
        applyMasks
    });

    document.addEventListener('DOMContentLoaded', () => applyMasks());
}());
