(function () {
    'use strict';

    const DATE_POSITIONS = Object.freeze([0, 1, 3, 4, 6, 7, 8, 9]);
    const dateMaskStates = new WeakMap();

    function digitsOnly(value, maximum) {
        return String(value || '').replace(/\D/g, '').slice(0, maximum);
    }

    function titleCase(value) {
        return String(value || '')
            .normalize('NFC')
            .toLocaleLowerCase('pt-BR')
            .replace(/(^|[^\p{L}\p{N}])(\p{L})/gu, (match, prefix, letter) =>
                prefix + letter.toLocaleUpperCase('pt-BR'));
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
        return text;
    }

    function dateToIso(value) {
        const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(String(value || '').trim());
        return match ? `${match[3]}-${match[2]}-${match[1]}` : '';
    }

    function isValidDate(value) {
        const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(String(value || '').trim());
        if (!match) return false;

        const day = Number(match[1]);
        const month = Number(match[2]);
        const year = Number(match[3]);
        if (month < 1 || month > 12 || day < 1) return false;

        const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
        const daysByMonth = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        return day <= daysByMonth[month - 1];
    }

    function createDateMaskState(value) {
        const digits = digitsOnly(value, 8);
        const slots = Array(8).fill('');
        [...digits].forEach((digit, index) => slots[index] = digit);
        return { slots, rendered: renderDateMask(slots) };
    }

    function renderDateMask(slots) {
        if (slots.every(digit => !digit)) return '';
        return `${slots[0] || '_'}${slots[1] || '_'}/${slots[2] || '_'}${slots[3] || '_'}/`
            + `${slots[4] || '_'}${slots[5] || '_'}${slots[6] || '_'}${slots[7] || '_'}`;
    }

    function dateSlotAtOrAfter(position) {
        return DATE_POSITIONS.findIndex(editablePosition => editablePosition >= position);
    }

    function clearSelectedDateSlots(slots, selectionStart, selectionEnd) {
        DATE_POSITIONS.forEach((position, slot) => {
            if (position >= selectionStart && position < selectionEnd) slots[slot] = '';
        });
    }

    function editDateMask(state, selectionStart, selectionEnd, inputType, data = '') {
        const slots = [...state.slots];
        const hasSelection = selectionEnd > selectionStart;
        let caret = selectionStart;

        if (inputType === 'deleteContentBackward' || inputType === 'deleteContentForward') {
            if (hasSelection) {
                clearSelectedDateSlots(slots, selectionStart, selectionEnd);
            } else if (inputType === 'deleteContentBackward') {
                for (let slot = DATE_POSITIONS.length - 1; slot >= 0; slot -= 1) {
                    if (DATE_POSITIONS[slot] < selectionStart) {
                        slots[slot] = '';
                        caret = DATE_POSITIONS[slot];
                        break;
                    }
                }
            } else {
                const slot = dateSlotAtOrAfter(selectionStart);
                if (slot >= 0) {
                    slots[slot] = '';
                    caret = DATE_POSITIONS[slot];
                }
            }
        } else {
            const insertedDigits = digitsOnly(data, 8);
            if (!insertedDigits) {
                const nextSection = DATE_POSITIONS.find(position => position > selectionStart + 1);
                caret = nextSection === undefined ? selectionStart : nextSection;
                const rendered = renderDateMask(slots);
                return { state: { slots, rendered }, value: rendered, caret };
            }

            if (hasSelection) clearSelectedDateSlots(slots, selectionStart, selectionEnd);
            let slot = dateSlotAtOrAfter(selectionStart);
            if (slot < 0) slot = DATE_POSITIONS.length;

            for (const digit of insertedDigits) {
                if (slot >= slots.length) break;
                slots[slot] = digit;
                slot += 1;
            }
            caret = slot < DATE_POSITIONS.length ? DATE_POSITIONS[slot] : 10;
        }

        const rendered = renderDateMask(slots);
        if (!rendered) caret = 0;
        return { state: { slots, rendered }, value: rendered, caret };
    }

    function setDateMaskResult(input, result) {
        dateMaskStates.set(input, result.state);
        input.value = result.value;
        input.setSelectionRange(result.caret, result.caret);
    }

    function synchronizedDateState(input) {
        const current = dateMaskStates.get(input);
        if (current && current.rendered === input.value) return current;
        const state = createDateMaskState(input.value);
        dateMaskStates.set(input, state);
        return state;
    }

    function applyDateMask(input) {
        const initial = createDateMaskState(input.value);
        dateMaskStates.set(input, initial);
        input.value = initial.rendered;

        const updateValidity = () => {
            const invalid = input.value && !isValidDate(input.value);
            input.setCustomValidity(invalid ? 'Informe uma data válida no formato DD/MM/AAAA.' : '');
        };

        input.addEventListener('beforeinput', event => {
            const supportedTypes = new Set([
                'insertText', 'insertReplacementText', 'insertFromPaste',
                'deleteContentBackward', 'deleteContentForward',
                'deleteByCut', 'deleteByDrag'
            ]);
            if (!supportedTypes.has(event.inputType)) return;

            event.preventDefault();
            const result = editDateMask(
                synchronizedDateState(input),
                input.selectionStart || 0,
                input.selectionEnd || 0,
                event.inputType.startsWith('delete')
                    ? (event.inputType === 'deleteContentBackward'
                        ? 'deleteContentBackward'
                        : 'deleteContentForward')
                    : 'insertText',
                event.data || ''
            );
            setDateMaskResult(input, result);
            input.dispatchEvent(new Event('input', { bubbles: true }));
        });

        input.addEventListener('paste', event => {
            event.preventDefault();
            const result = editDateMask(
                synchronizedDateState(input),
                input.selectionStart || 0,
                input.selectionEnd || 0,
                'insertText',
                event.clipboardData.getData('text')
            );
            setDateMaskResult(input, result);
            input.dispatchEvent(new Event('input', { bubbles: true }));
        });

        input.addEventListener('input', () => {
            const current = dateMaskStates.get(input);
            if (current && current.rendered === input.value) return;

            const caret = input.selectionStart || 0;
            const digitsBeforeCaret = digitsOnly(input.value.slice(0, caret), 8).length;
            const state = createDateMaskState(input.value);
            dateMaskStates.set(input, state);
            input.value = state.rendered;
            const nextCaret = digitsBeforeCaret < DATE_POSITIONS.length
                ? DATE_POSITIONS[digitsBeforeCaret]
                : state.rendered.length;
            input.setSelectionRange(nextCaret, nextCaret);
        });
        input.addEventListener('input', updateValidity);
        input.addEventListener('blur', updateValidity);
    }

    function applySimpleMask(input, formatter) {
        input.value = formatter(input.value);
        input.addEventListener('input', () => {
            input.value = formatter(input.value);
        });
    }

    function applyTitleCase(input) {
        input.value = titleCase(input.value);
        input.addEventListener('input', () => {
            const start = input.selectionStart;
            const end = input.selectionEnd;
            const original = input.value;
            input.value = titleCase(original);
            if (start !== null && end !== null) {
                const formattedStart = titleCase(original.slice(0, start)).length;
                const formattedEnd = titleCase(original.slice(0, end)).length;
                input.setSelectionRange(formattedStart, formattedEnd);
            }
        });
    }

    function applyMasks(root = document) {
        root.querySelectorAll('[data-mask="phone"]').forEach(input =>
            applySimpleMask(input, formatPhoneInput));
        root.querySelectorAll('[data-mask="date"]').forEach(applyDateMask);
        root.querySelectorAll('[data-format="title-case"]').forEach(applyTitleCase);

        root.querySelectorAll('form').forEach(form => {
            form.addEventListener('submit', () => {
                form.querySelectorAll('[data-format="title-case"]').forEach(input => {
                    input.value = titleCase(input.value.trim());
                });
            });
        });
    }

    const api = Object.freeze({
        titleCase,
        normalizePhone,
        formatPhone,
        formatPhoneInput,
        formatDate,
        formatDateInput,
        dateToIso,
        isValidDate,
        createDateMaskState,
        editDateMask,
        applyMasks
    });

    if (typeof window !== 'undefined') window.DoaLocalFormats = api;
    if (typeof module !== 'undefined' && module.exports) module.exports = api;
    if (typeof document !== 'undefined') {
        document.addEventListener('DOMContentLoaded', () => applyMasks());
    }
}());
