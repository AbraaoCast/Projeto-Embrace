package br.com.doalocal.util;

import br.com.doalocal.model.DoacaoEstoque;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DoacaoJsonTest {

    @Test
    void serializesOriginalDeliveredAndAvailableQuantities() {
        String json = DoacaoJson.stockList(List.of(new DoacaoEstoque(
                7, "Roupas", "Camisetas", 5, 1, 4,
                LocalDate.of(2026, 9, 23), null
        )));

        assertTrue(json.contains("\"quantidadeOriginal\":5"));
        assertTrue(json.contains("\"quantidadeEntregue\":1"));
        assertTrue(json.contains("\"quantidadeDisponivel\":4"));
        assertTrue(json.contains("\"dataDoacao\":\"23/09/2026\""));
    }
}
