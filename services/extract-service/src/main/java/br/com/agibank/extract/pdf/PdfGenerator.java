package br.com.agibank.extract.pdf;

import br.com.agibank.extract.model.dto.ExtractResponse;
import br.com.agibank.extract.model.dto.TransactionDTO;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class PdfGenerator {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] gerarPdf(ExtractResponse extrato) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("AGIBANK - EXTRATO BANCÁRIO")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(" "));

            document.add(new Paragraph("Conta: " + extrato.numeroConta()).setFontSize(12));
            document.add(new Paragraph("Período: " + extrato.dataInicial().format(DATE_FORMAT) +
                    " a " + extrato.dataFinal().format(DATE_FORMAT)).setFontSize(12));
            document.add(new Paragraph("Saldo Inicial: " + formatCurrency(extrato.saldoInicial())).setFontSize(12));
            document.add(new Paragraph("Saldo Final: " + formatCurrency(extrato.saldoFinal())).setFontSize(12));
            document.add(new Paragraph("Total de Transações: " + extrato.totalTransacoes()).setFontSize(12));

            document.add(new Paragraph(" "));

            if (!extrato.transacoes().isEmpty()) {
                Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1.5f, 3, 2, 2}));
                table.setWidth(UnitValue.createPercentValue(100));

                table.addHeaderCell(new Paragraph("Data/Hora").setBold());
                table.addHeaderCell(new Paragraph("Tipo").setBold());
                table.addHeaderCell(new Paragraph("Descrição").setBold());
                table.addHeaderCell(new Paragraph("Valor").setBold());
                table.addHeaderCell(new Paragraph("Saldo").setBold());

                for (TransactionDTO transacao : extrato.transacoes()) {
                    table.addCell(transacao.dataHora().format(DATETIME_FORMAT));
                    table.addCell(transacao.tipo());
                    table.addCell(transacao.descricao() != null ? transacao.descricao() : "-");
                    table.addCell(formatCurrency(transacao.valor()));
                    table.addCell(formatCurrency(transacao.saldoAtual()));
                }

                document.add(table);
            } else {
                document.add(new Paragraph("Nenhuma transação encontrada no período.")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Documento gerado automaticamente pelo sistema Agibank")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF do extrato", e);
        }
    }

    private String formatCurrency(BigDecimal value) {
        return value != null ? CURRENCY_FORMAT.format(value) : "R$ 0,00";
    }
}
