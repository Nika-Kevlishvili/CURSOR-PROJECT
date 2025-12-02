package bg.energo.phoenix.service.document;

import bg.energo.phoenix.exception.ClientException;
import com.aspose.words.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Slf4j
@Service
public class DocumentParserService {

    /**
     * Parses a DOCX file content into a string representation, including both text and embedded images.
     * Images are converted to Base64 format and included in the output string.
     * Each paragraph is separated by a newline character and preserves original document spacing.
     *
     * @param content byte array containing the DOCX file content
     * @return String containing the parsed text and Base64-encoded images
     * @throws ClientException if the content is null or parsing fails
     */
    public static String parseDocx(byte[] content) throws IOException {
        log.info("Starting DOCX to HTML conversion process");
        if (content == null) {
            log.error("Received null content for DOCX parsing");
            throw new ClientException("File content is empty", APPLICATION_ERROR);
        }
        log.info("Received DOCX content with length: " + content.length);
        try {
            log.info("Initializing ByteArrayInputStream for DOCX content");
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
            log.info("Creating Document object from ByteArrayInputStream");
            Document asposeDocument = new Document(byteArrayInputStream);

            StringBuilder text = new StringBuilder();
            for (Object obj : asposeDocument.getChildNodes(NodeType.PARAGRAPH, true))
            {
                Paragraph para = (Paragraph)obj;
                text.append(para.getText());
                text.append("\n");
            }
            return text.toString();
        } catch (Exception e) {
            log.error("Failed to parse DOCX file", e);
            throw new ClientException("Failed to parse DOCX file: " + e.getMessage(), APPLICATION_ERROR);
        }
    }

    public static String parseDocxToHtml(byte[] content) {
        log.info("Starting DOCX to HTML conversion process");

        if (content == null) {
            log.error("Received null content for DOCX parsing");
            throw new ClientException("File content is empty", APPLICATION_ERROR);
        }

        log.info("Received DOCX content with length: " + content.length);

        try {
            log.info("Initializing ByteArrayInputStream for DOCX content");
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);

            log.info("Creating Document object from ByteArrayInputStream");
            Document asposeDocument = new Document(byteArrayInputStream);

            log.info("Setting HTML save options");
            HtmlSaveOptions options = new HtmlSaveOptions();
            options.setSaveFormat(SaveFormat.HTML);
            options.setExportImagesAsBase64(true);

            log.info("Preparing ByteArrayOutputStream to capture HTML content");
            ByteArrayOutputStream htmlOutput = new ByteArrayOutputStream();

            log.info("Saving DOCX document as HTML...");
            asposeDocument.save(htmlOutput, options);

            log.info("HTML conversion completed, length of generated HTML: " + htmlOutput.size());

            String htmlContent = htmlOutput.toString(StandardCharsets.UTF_8);
            String htmlBodyContent = extractHtmlBodyContent(htmlContent);
            log.info("Successfully converted DOCX to HTML");

            if (StringUtils.isNotEmpty(htmlBodyContent)) {
                return htmlBodyContent;
            } else {
                return htmlContent;
            }

        } catch (Exception e) {
            log.error("Failed to parse DOCX file to HTML", e);
            throw new ClientException("Failed to convert DOCX file to HTML: " + e.getMessage(), APPLICATION_ERROR);
        }
    }

    public static String extractHtmlBodyContent(String htmlContent) {
        String bodyPattern = "(<body[^>]*>.*?</body>)";

        Pattern pattern = Pattern.compile(bodyPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(htmlContent);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public static String parsePdf(byte[] content) {
        try {
            File originalFile = new File(Files.createTempFile("cur", ".pdf").toUri());

            Files.write(originalFile.toPath(), content);

            try (PDDocument document = PDDocument.load(originalFile)) {
                PDFTextStripper stripper = new PDFTextStripper();

                return stripper.getText(document);
            }
        } catch (Exception e) {
            log.error("Failed to parse PDF file", e);
            throw new ClientException("Failed to parse PDF file: " + e.getMessage(), APPLICATION_ERROR);
        }
    }
}
