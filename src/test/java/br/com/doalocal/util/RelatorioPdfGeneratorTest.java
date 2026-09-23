package br.com.doalocal.util;

import br.com.doalocal.model.RelatorioEntregaItem;
import br.com.doalocal.model.RelatorioFiltro;
import br.com.doalocal.model.RelatorioQuantidade;
import br.com.doalocal.model.RelatorioResultado;
import br.com.doalocal.model.RelatorioResumo;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioPdfGeneratorTest {

    private final RelatorioPdfGenerator generator = new RelatorioPdfGenerator();

    @Test
    void createsValidNonEmptyPdfWithPortugueseContent() throws IOException {
        byte[] pdf = generator.generate(reportWithItems(1));
        assertTrue(pdf.length > 1_000);
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("RELATÓRIO DE ENTREGAS"));
            assertTrue(text.contains("Maria São Luís"));
            assertTrue(text.contains("Doação solidária"));
            assertTrue(text.contains("Observação com acentuação"));
            assertTrue(text.contains("22/09/2026"));
            assertTrue(text.contains("Página 1 de 1"));
        }
    }

    @Test
    void createsValidPdfWhenReportHasNoResults() throws IOException {
        RelatorioResultado empty = new RelatorioResultado(
                new RelatorioFiltro(null, null, null, null),
                new RelatorioResumo(0, 0, 0, 0),
                List.of(),
                List.of()
        );
        byte[] pdf = generator.generate(empty);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Nenhuma entrega encontrada"));
        }
    }

    @Test
    void paginatesFiveHundredRowsWithoutLosingEntries() throws IOException {
        byte[] pdf = generator.generate(reportWithItems(500));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() > 10);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Registro 1 "));
            assertTrue(text.contains("Registro 500 "));
            assertTrue(text.contains("Página 1 de "));
            assertTrue(text.contains("Página " + document.getNumberOfPages() + " de "
                    + document.getNumberOfPages()));
        }
    }

    @Test
    void createsSafeDescriptiveFileNames() {
        assertEquals("doalocal-relatorio-entregas-2026-09.pdf", generator.fileName(
                new RelatorioFiltro(
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, null
                )
        ));
        assertEquals("doalocal-relatorio-entregas-completo.pdf", generator.fileName(
                new RelatorioFiltro(null, null, null, null)
        ));
    }

    private static RelatorioResultado reportWithItems(int count) {
        List<RelatorioEntregaItem> items = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            items.add(new RelatorioEntregaItem(
                    index,
                    LocalDate.of(2026, 9, 22),
                    index % 3 + 1,
                    "Maria São Luís Registro " + index + " nome suficientemente longo",
                    "52998224725",
                    4,
                    1,
                    "Alimentos",
                    "Doação solidária com descrição longa " + index,
                    2,
                    "Observação com acentuação: ação, órgão, João, família e doação."
            ));
        }
        return new RelatorioResultado(
                new RelatorioFiltro(
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, null
                ),
                new RelatorioResumo(3, 1, count, Math.min(3, count)),
                List.of(new RelatorioQuantidade(1, "Alimentos - Doação solidária", count * 2)),
                List.copyOf(items)
        );
    }
}
