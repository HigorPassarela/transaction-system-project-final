package br.com.agibank.extract.pdf;


import br.com.agibank.extract.model.dto.ExtractResponse;
import br.com.agibank.extract.model.dto.TransactionDTO;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class PdfGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerator.class);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] gerarPdf(ExtractResponse extrato) {
        logger.info("Gerando PDF para extrato da conta {}", extrato.numeroConta());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Cabeçalho
            adicionarCabecalho(document);

            // Informações da conta
            adicionarInformacoesConta(document, extrato);

            // Resumo financeiro
            adicionarResumoFinanceiro(document, extrato);

            // Tabela de transações
            if (!extrato.transacoes().isEmpty()) {
                adicionarTabelaTransacoes(document, extrato);
            } else {
                adicionarMensagemSemTransacoes(document);
            }

            // Rodapé
            adicionarRodape(document);

            document.close();

            logger.info("PDF gerado com sucesso - {} bytes", baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Erro ao gerar PDF do extrato: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar PDF do extrato", e);
        }
    }

    private void adicionarCabecalho(Document document) {
        Paragraph titulo = new Paragraph("🏦 AGIBANK - EXTRATO BANCÁRIO")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);

        document.add(titulo);

        Paragraph linha = new Paragraph("═".repeat(80))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);

        document.add(linha);
    }

    private void adicionarInformacoesConta(Document document, ExtractResponse extrato) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(15);

        // Coluna esquerda
        infoTable.addCell(createInfoCell("Conta:", extrato.numeroConta()));
        infoTable.addCell(createInfoCell("Período:",
                extrato.dataInicial().format(DATE_FORMAT) + " a " + extrato.dataFinal().format(DATE_FORMAT)));

        infoTable.addCell(createInfoCell("Total de Transações:", String.valueOf(extrato.totalTransacoes())));
        infoTable.addCell(createInfoCell("Data de Geração:",
                extrato.dataGeracao().format(DATETIME_FORMAT)));

        document.add(infoTable);
    }

    private void adicionarResumoFinanceiro(Document document, ExtractResponse extrato) {
        Table resumoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        resumoTable.setWidth(UnitValue.createPercentValue(100));
        resumoTable.setMarginBottom(20);

        // Saldo inicial
        Cell saldoInicialLabel = new Cell().add(new Paragraph("Saldo Inicial:").setBold());
        Cell saldoInicialValue = new Cell().add(new Paragraph(formatCurrency(extrato.saldoInicial())));
        resumoTable.addCell(saldoInicialLabel);
        resumoTable.addCell(saldoInicialValue);

        // Saldo final
        Cell saldoFinalLabel = new Cell().add(new Paragraph("Saldo Final:").setBold());
        Cell saldoFinalValue = new Cell().add(new Paragraph(formatCurrency(extrato.saldoFinal())).setBold());

        // Colorir saldo final
        if (extrato.saldoFinal().compareTo(BigDecimal.ZERO) < 0) {
            saldoFinalValue.setFontColor(ColorConstants.RED);
        } else {
            saldoFinalValue.setFontColor(ColorConstants.GREEN);
        }

        resumoTable.addCell(saldoFinalLabel);
        resumoTable.addCell(saldoFinalValue);

        document.add(resumoTable);
    }

    private void adicionarTabelaTransacoes(Document document, ExtractResponse extrato) {
        Paragraph tituloTransacoes = new Paragraph("📋 MOVIMENTAÇÕES")
                .setFontSize(14)
                .setBold()
                .setMarginBottom(10);

        document.add(tituloTransacoes);

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 1, 2, 1.5f, 1.5f}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Cabeçalho da tabela
        table.addHeaderCell(createHeaderCell("Data/Hora"));
        table.addHeaderCell(createHeaderCell("Tipo"));
        table.addHeaderCell(createHeaderCell("Descrição"));
        table.addHeaderCell(createHeaderCell("Valor"));
        table.addHeaderCell(createHeaderCell("Saldo"));

        // Dados das transações
        for (TransactionDTO transacao : extrato.transacoes()) {
            table.addCell(createDataCell(transacao.dataHora().format(DATETIME_FORMAT)));

            // Tipo com ícone
            String tipoComIcone = "DEBITO".equals(transacao.tipo()) ? "🔻 " + transacao.tipo() : "🔺 " + transacao.tipo();
            table.addCell(createDataCell(tipoComIcone));

            table.addCell(createDataCell(transacao.descricao() != null ? transacao.descricao() : "-"));

            // Valor com cor
            Cell valorCell = createDataCell(formatCurrency(transacao.valor()));
            if ("DEBITO".equals(transacao.tipo())) {
                valorCell.setFontColor(ColorConstants.RED);
            } else {
                valorCell.setFontColor(ColorConstants.BLUE);
            }
            table.addCell(valorCell);

            table.addCell(createDataCell(formatCurrency(transacao.saldoAtual())));
        }

        document.add(table);
    }

    private void adicionarMensagemSemTransacoes(Document document) {
        Paragraph mensagem = new Paragraph("ℹ️ Nenhuma transação encontrada no período informado.")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12)
                .setMarginTop(30)
                .setMarginBottom(30);

        document.add(mensagem);
    }

    private void adicionarRodape(Document document) {
        document.add(new Paragraph(" ").setMarginTop(20));

        Paragraph linha = new Paragraph("═".repeat(80))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);

        document.add(linha);

        Paragraph rodape = new Paragraph("Documento gerado automaticamente pelo sistema Agibank")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER);

        document.add(rodape);

        Paragraph timestamp = new Paragraph("Gerado em: " + java.time.LocalDateTime.now().format(DATETIME_FORMAT))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER);

        document.add(timestamp);
    }

    private Cell createInfoCell(String label, String value) {
        Paragraph p = new Paragraph()
                .add(new Paragraph(label).setBold())
                .add(" " + value);
        return new Cell().add(p).setBorder(null).setPadding(5);
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    private Cell createDataCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setPadding(5)
                .setTextAlignment(TextAlignment.LEFT);
    }

    private String formatCurrency(BigDecimal value) {
        return value != null ? CURRENCY_FORMAT.format(value) : "R$ 0,00";
    }
}
