package br.com.doalocal.util;

import br.com.doalocal.exception.PdfGenerationException;
import br.com.doalocal.model.RelatorioEntregaItem;
import br.com.doalocal.model.RelatorioFiltro;
import br.com.doalocal.model.RelatorioQuantidade;
import br.com.doalocal.model.RelatorioResultado;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.util.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Cria o relatório de Entregas em PDF paginado diretamente em memória. */
public final class RelatorioPdfGenerator {

    private static final float PAGE_WIDTH = PDRectangle.A4.getHeight();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getWidth();
    private static final float MARGIN = 40;
    private static final float BOTTOM = 38;
    private static final float[] COLUMN_WIDTHS = {62, 145, 165, 58, 292};
    private static final String[] HEADERS = {"Data", "Família / CPF", "Doação", "Quantidade", "Observação"};
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDColor NAVY = new PDColor(new float[]{0.06f, 0.10f, 0.18f}, PDDeviceRGB.INSTANCE);
    private static final PDColor BLUE = new PDColor(new float[]{0.20f, 0.48f, 0.68f}, PDDeviceRGB.INSTANCE);
    private static final PDColor LIGHT = new PDColor(new float[]{0.93f, 0.95f, 0.97f}, PDDeviceRGB.INSTANCE);
    private static final PDColor GRAY = new PDColor(new float[]{0.35f, 0.39f, 0.45f}, PDDeviceRGB.INSTANCE);
    private static final PDColor WHITE = new PDColor(new float[]{1, 1, 1}, PDDeviceRGB.INSTANCE);

    public byte[] generate(RelatorioResultado report) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Writer writer = new Writer(document, report);
            writer.startPage();
            writer.drawSummary();
            writer.drawGroupedQuantities();
            writer.drawTable();
            writer.closeCurrentStream();
            writer.drawFooters();
            document.save(output);
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException exception) {
            throw new PdfGenerationException("Não foi possível gerar o relatório em PDF.", exception);
        }
    }

    public String fileName(RelatorioFiltro filter) {
        if (filter.dataInicial() != null && filter.dataFinal() != null) {
            String start = filter.dataInicial().toString();
            String end = filter.dataFinal().toString();
            return start.substring(0, 7).equals(end.substring(0, 7))
                    ? "doalocal-relatorio-entregas-" + start.substring(0, 7) + ".pdf"
                    : "doalocal-relatorio-entregas-" + start + "-a-" + end + ".pdf";
        }
        return "doalocal-relatorio-entregas-completo.pdf";
    }

    private static final class Writer {
        private final PDDocument document;
        private final RelatorioResultado report;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private Writer(PDDocument document, RelatorioResultado report) {
            this.document = document;
            this.report = report;
        }

        private void startPage() throws IOException {
            closeCurrentStream();
            page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            drawText("DOALOCAL", BOLD, 16, MARGIN, PAGE_HEIGHT - MARGIN, BLUE);
            drawText("RELATÓRIO DE ENTREGAS", BOLD, 15, MARGIN, PAGE_HEIGHT - MARGIN - 25, NAVY);
            drawText("Período: " + periodLabel(report.filtro()), REGULAR, 9,
                    MARGIN, PAGE_HEIGHT - MARGIN - 43, GRAY);
            drawText("Gerado em: " + DateFormats.formatUserDate(LocalDate.now()), REGULAR, 9,
                    PAGE_WIDTH - MARGIN - 145, PAGE_HEIGHT - MARGIN - 43, GRAY);
            y = PAGE_HEIGHT - MARGIN - 65;
        }

        private void drawSummary() throws IOException {
            ensureSpace(58);
            stream.setNonStrokingColor(LIGHT);
            stream.addRect(MARGIN, y - 48, PAGE_WIDTH - 2 * MARGIN, 48);
            stream.fill();
            drawText("RESUMO", BOLD, 9, MARGIN + 10, y - 14, NAVY);
            String text = "Entregas: " + report.resumo().totalEntregas()
                    + "    Famílias atendidas: " + report.resumo().familiasAtendidas()
                    + "    Famílias cadastradas: " + report.resumo().totalFamiliasCadastradas()
                    + "    Doações cadastradas: " + report.resumo().totalDoacoesCadastradas();
            drawText(text, REGULAR, 9, MARGIN + 10, y - 33, NAVY);
            y -= 60;
        }

        private void drawGroupedQuantities() throws IOException {
            if (report.quantidadesPorDoacao().isEmpty()) {
                return;
            }
            ensureSpace(28);
            drawText("QUANTIDADES DISTRIBUÍDAS POR DOAÇÃO", BOLD, 9, MARGIN, y, NAVY);
            y -= 15;
            for (RelatorioQuantidade quantity : report.quantidadesPorDoacao()) {
                List<String> lines = wrapText(
                        quantity.identificacao() + ": " + quantity.quantidade(),
                        REGULAR, 8, PAGE_WIDTH - 2 * MARGIN - 12
                );
                for (String line : lines) {
                    ensureSpace(12);
                    drawText("- " + line, REGULAR, 8, MARGIN + 8, y, GRAY);
                    y -= 11;
                }
            }
            y -= 8;
        }

        private void drawTable() throws IOException {
            drawTableHeader();
            if (report.itens().isEmpty()) {
                ensureSpace(30);
                drawText("Nenhuma entrega encontrada para os filtros informados.", REGULAR, 9,
                        MARGIN + 6, y - 17, GRAY);
                y -= 30;
                return;
            }

            for (RelatorioEntregaItem item : report.itens()) {
                List<List<String>> columns = rowColumns(item);
                int maximumLines = columns.stream().mapToInt(List::size).max().orElse(1);
                float rowHeight = Math.max(24, maximumLines * 9.5f + 8);
                if (y - rowHeight < BOTTOM + 12) {
                    startPage();
                    drawTableHeader();
                }
                drawRow(columns, rowHeight);
            }
        }

        private void drawTableHeader() throws IOException {
            ensureSpace(28);
            float height = 24;
            stream.setNonStrokingColor(NAVY);
            stream.addRect(MARGIN, y - height, PAGE_WIDTH - 2 * MARGIN, height);
            stream.fill();
            float x = MARGIN;
            for (int index = 0; index < HEADERS.length; index++) {
                drawText(HEADERS[index], BOLD, 7.5f, x + 4, y - 15, WHITE);
                x += COLUMN_WIDTHS[index];
            }
            y -= height;
        }

        private void drawRow(List<List<String>> columns, float height) throws IOException {
            float top = y;
            stream.setStrokingColor(new PDColor(new float[]{0.78f, 0.81f, 0.85f}, PDDeviceRGB.INSTANCE));
            stream.setLineWidth(0.4f);
            stream.addRect(MARGIN, top - height, PAGE_WIDTH - 2 * MARGIN, height);
            stream.stroke();

            float x = MARGIN;
            for (int column = 0; column < columns.size(); column++) {
                if (column > 0) {
                    stream.moveTo(x, top);
                    stream.lineTo(x, top - height);
                    stream.stroke();
                }
                float lineY = top - 11;
                for (String line : columns.get(column)) {
                    drawText(line, REGULAR, 7.5f, x + 4, lineY, NAVY);
                    lineY -= 9.5f;
                }
                x += COLUMN_WIDTHS[column];
            }
            y -= height;
        }

        private List<List<String>> rowColumns(RelatorioEntregaItem item) throws IOException {
            List<List<String>> columns = new ArrayList<>();
            columns.add(wrapText(DateFormats.formatUserDate(item.dataEntrega()), REGULAR, 7.5f,
                    COLUMN_WIDTHS[0] - 8));
            columns.add(wrapText(item.nomeResponsavel() + " (" +
                    CpfFormats.formatForDisplay(item.cpfResponsavel()) + ")", REGULAR, 7.5f,
                    COLUMN_WIDTHS[1] - 8));
            columns.add(wrapText(item.tipoDoacao() + " - " + item.descricaoDoacao(), REGULAR, 7.5f,
                    COLUMN_WIDTHS[2] - 8));
            columns.add(List.of(String.valueOf(item.quantidadeEntregue())));
            columns.add(wrapText(item.observacao() == null ? "-" : item.observacao(), REGULAR, 7.5f,
                    COLUMN_WIDTHS[4] - 8));
            return columns;
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < BOTTOM) {
                startPage();
            }
        }

        private void drawFooters() throws IOException {
            int total = document.getNumberOfPages();
            for (int index = 0; index < total; index++) {
                PDPage footerPage = document.getPage(index);
                try (PDPageContentStream footer = new PDPageContentStream(
                        document, footerPage, PDPageContentStream.AppendMode.APPEND, true, true
                )) {
                    String text = "Página " + (index + 1) + " de " + total;
                    float width = textWidth(text, REGULAR, 7.5f);
                    footer.beginText();
                    footer.setFont(REGULAR, 7.5f);
                    footer.setNonStrokingColor(GRAY);
                    footer.setTextMatrix(Matrix.getTranslateInstance((PAGE_WIDTH - width) / 2, 18));
                    footer.showText(safeText(text));
                    footer.endText();
                }
            }
        }

        private void drawText(
                String text,
                PDFont font,
                float size,
                float x,
                float textY,
                PDColor color
        ) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.setTextMatrix(Matrix.getTranslateInstance(x, textY));
            stream.showText(safeText(text));
            stream.endText();
        }

        private void closeCurrentStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }

    private static String periodLabel(RelatorioFiltro filter) {
        if (filter.dataInicial() == null && filter.dataFinal() == null) {
            return "Todo o histórico";
        }
        String start = filter.dataInicial() == null
                ? "início" : DateFormats.formatUserDate(filter.dataInicial());
        String end = filter.dataFinal() == null
                ? "hoje" : DateFormats.formatUserDate(filter.dataFinal());
        return start + " a " + end;
    }

    private static List<String> wrapText(
            String value,
            PDFont font,
            float fontSize,
            float maximumWidth
    ) throws IOException {
        String normalized = safeText(value == null ? "" : value.replaceAll("\\s+", " ").trim());
        if (normalized.isEmpty()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : normalized.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (textWidth(candidate, font, fontSize) <= maximumWidth) {
                line.setLength(0);
                line.append(candidate);
            } else if (!line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
                appendLongWord(lines, line, word, font, fontSize, maximumWidth);
            } else {
                appendLongWord(lines, line, word, font, fontSize, maximumWidth);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static void appendLongWord(
            List<String> lines,
            StringBuilder line,
            String word,
            PDFont font,
            float fontSize,
            float maximumWidth
    ) throws IOException {
        for (char character : word.toCharArray()) {
            String candidate = line.toString() + character;
            if (!line.isEmpty() && textWidth(candidate, font, fontSize) > maximumWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(character);
        }
    }

    private static float textWidth(String value, PDFont font, float size) throws IOException {
        return font.getStringWidth(safeText(value)) / 1000 * size;
    }

    private static String safeText(String value) throws IOException {
        StringBuilder safe = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            String character = String.valueOf(value.charAt(index));
            try {
                REGULAR.encode(character);
                safe.append(character);
            } catch (IllegalArgumentException exception) {
                safe.append('?');
            }
        }
        return safe.toString();
    }
}
