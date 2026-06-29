package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * PDF 文档解析器，基于 Apache PDFBox 提取纯文本内容。
 *
 * @author opensnail
 * @since 2026-06-14
 */
@Slf4j
@Component
public class PdfParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }

    @Override
    public String parse(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            throw new SnailAiException("Failed to parse PDF", e);
        }
    }
}
