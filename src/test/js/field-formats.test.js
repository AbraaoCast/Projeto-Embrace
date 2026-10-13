const test = require('node:test');
const assert = require('node:assert/strict');
const formats = require('../../main/webapp/script/field-formats.js');

test('capitaliza nomes e endereços sem remover caracteres válidos', () => {
    assert.equal(formats.titleCase('joão da silva'), 'João Da Silva');
    assert.equal(formats.titleCase('sa\u0303o paulo'), 'São Paulo');
    assert.equal(formats.titleCase('avenida 9 de julho'), 'Avenida 9 De Julho');
    assert.equal(formats.titleCase("d'ávila"), "D'Ávila");
});

test('substitui somente o algarismo selecionado da data', () => {
    const state = formats.createDateMaskState('23/09/2026');
    const result = formats.editDateMask(state, 1, 2, 'insertText', '4');

    assert.equal(result.value, '24/09/2026');
    assert.equal(result.caret, 3);
});

test('backspace e delete removem o algarismo na posição do cursor', () => {
    const state = formats.createDateMaskState('23/09/2026');
    const backspace = formats.editDateMask(state, 2, 2, 'deleteContentBackward');
    const deletion = formats.editDateMask(state, 3, 3, 'deleteContentForward');

    assert.equal(backspace.value, '2_/09/2026');
    assert.equal(backspace.caret, 1);
    assert.equal(deletion.value, '23/_9/2026');
    assert.equal(deletion.caret, 3);
});

test('permite substituir uma seção e colar uma data completa', () => {
    const state = formats.createDateMaskState('23/09/2026');
    const year = formats.editDateMask(state, 6, 10, 'insertText', '2027');
    const pasted = formats.editDateMask(formats.createDateMaskState(''), 0, 0,
        'insertText', '31/12/2028');

    assert.equal(year.value, '23/09/2027');
    assert.equal(pasted.value, '31/12/2028');
});

test('valida formato e dias reais do calendário', () => {
    assert.equal(formats.isValidDate('29/02/2024'), true);
    assert.equal(formats.isValidDate('29/02/2025'), false);
    assert.equal(formats.isValidDate('31/04/2026'), false);
    assert.equal(formats.isValidDate('2026-09-23'), false);
});
