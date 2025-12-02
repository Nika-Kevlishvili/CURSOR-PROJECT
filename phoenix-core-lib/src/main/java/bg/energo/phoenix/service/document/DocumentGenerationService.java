package bg.energo.phoenix.service.document;

import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.formaters.*;
import bg.energo.phoenix.service.document.ftpService.FileService;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import hr.ngs.templater.Configuration;
import hr.ngs.templater.DocumentFactory;
import hr.ngs.templater.TemplateDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGenerationService {
    private final FileService fileService;

    @PostConstruct
    public void configLicense() {
        try {
            log.debug("StartingLoadingPdfLicense");
            com.aspose.words.License license = new com.aspose.words.License();
            license.setLicense(new ClassPathResource("Aspose.WordsforJava.lic").getInputStream());
            log.debug("Finished loading pdf license;");
            //ProcessReport.init();
        } catch (Exception e) {
            log.debug("Error while loading pdf license;", e);
        }
    }

    public <Model> DocumentPathPayloads generateDocument(ByteArrayResource templateByteArrayResource,
                                                         String remoteTargetPath,
                                                         String fileName,
                                                         Model model,
                                                         Set<FileFormat> fileFormats,
                                                         boolean addDraftWatermark) throws Exception {
        return startGenerationProcess(templateByteArrayResource, remoteTargetPath, fileName, model, fileFormats, addDraftWatermark);
    }

    private <Model> DocumentPathPayloads startGenerationProcess(ByteArrayResource templateByteArrayResource,
                                                                String remoteTargetPath,
                                                                String fileName,
                                                                Model model,
                                                                Set<FileFormat> fileFormats,
                                                                boolean addDraftWatermark) throws Exception {
        log.debug("Start generation process");
        String docPath = null;
        String pdfPath = null;
        String excelPath = null;

        try (InputStream template = templateByteArrayResource.getInputStream()) {
            log.debug("Creating document factory");
            DocumentFactory factory = Configuration
                    .builder()
                    .navigateSeparator('!', null)
                    .include(new ConvertQR())//setup QR code generation from text
                    .include(new CollapseIf())//setup QR code generation from text
                    .include(new CollapseControlIfInArray())//show if control
                    .include(new ShowControlIf())//show if control
                    .include(new ShowControlIfInArray())//show if control
                    .include(new ConvertIMG()) // draw image
                    .include(new ConvertDateFormat()) // format date
                    .onUnprocessed(new TemplatorUnprocessedHandler()) // hide unprocessed tags
                    .build();

            log.debug("Creating temp file");

            if (fileFormats.contains(FileFormat.XLSX)) {
                excelPath = generateXLSXDoc(remoteTargetPath, fileName, model, factory, template);
            }

            if (fileFormats.contains(FileFormat.PDF) || fileFormats.contains(FileFormat.DOCX)) {
                File docxTempModel = File.createTempFile("cur", ".docx");

                log.debug("Writing template into temp file");
                try (FileOutputStream fos = new FileOutputStream(docxTempModel); TemplateDocument tpl = factory.open(template, "docx", fos)) {
                    tpl.process(model);
                    log.debug("Template temp file was formatted successfully");
                }

                log.debug("File formats: [%s]".formatted(fileFormats));
                for (FileFormat fileFormat : fileFormats) {
                    switch (fileFormat) {
                        case DOCX ->
                                docPath = generateDocX(docxTempModel, remoteTargetPath, "%s_%s".formatted(UUID.randomUUID(), fileName));
                        case PDF ->
                                pdfPath = generatePDF(docxTempModel, remoteTargetPath, "%s_%s".formatted(UUID.randomUUID(), fileName), addDraftWatermark);
                    }
                }
            }
        }

        return new DocumentPathPayloads(docPath, pdfPath, excelPath);
    }

    private <Model> String generateXLSXDoc(String remoteTargetPath, String fileName, Model model, DocumentFactory factory, InputStream template) throws IOException {
        File tmp = File.createTempFile("cur", ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(tmp);
             TemplateDocument tpl = factory.open(template, "xlsx", fos)) {
            tpl.process(model);
        }
        fileName = checkFileExtension(fileName, ".xlsx");
        return fileService.uploadFile(tmp, remoteTargetPath, fileName);
    }

    private String generateDocX(File generatedDocument, String remoteTargetPath, String fileName) {
        fileName = checkFileExtension(fileName, ".docx");
        return fileService.uploadFile(generatedDocument, remoteTargetPath, fileName);
    }

    private String generatePDF(File generatedDocument, String remoteTargetPath, String fileName, boolean addDraftWatermark) throws Exception {
        log.debug("Creating PDF document");
        Document document = new Document(new FileInputStream(generatedDocument));
        File tmpPdf = File.createTempFile("cur", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tmpPdf)) {
            document.save(fos, SaveFormat.PDF);
            log.debug("PDF document was created");

            if (addDraftWatermark) {
                tmpPdf = addTextWatermarkToPdfFile(tmpPdf, "DRAFT");
            }

            fileName = checkFileExtension(fileName, ".pdf");
            String ftpFileLocation = fileService.uploadFile(tmpPdf, remoteTargetPath, fileName);
            log.debug("PDF document was uploaded successfully, full path from FTP: [%s]".formatted(ftpFileLocation));
            return ftpFileLocation;
        }
    }

    private String checkFileExtension(String fileName, String extension) {
        if (!fileName.endsWith(extension)) {
            fileName += extension;
        }
        return fileName;
    }

    public File addTextWatermarkToPdfFile(File sourcePdf, String watermarkText) throws IOException {
        File outputFile = File.createTempFile("watermarked_" + sourcePdf.getName(), ".pdf");

        // Load the PDF document
        try (PDDocument document = PDDocument.load(sourcePdf)) {
            for (PDPage page : document.getPages()) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();

                // Open the content stream for writing on the page
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, true, true)) {
                    // Set font, color, rotation and watermark text
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 50);
                    contentStream.setNonStrokingColor(220, 220, 220); // Light gray
                    contentStream.beginText();
                    contentStream.setTextRotation(Math.toRadians(45), pageWidth / 2, pageHeight / 2); // Rotate diagonally
                    contentStream.showText(watermarkText);
                    contentStream.endText();
                }
            }

            // Save the file with the watermark
            document.save(outputFile);
        }
        return outputFile;
    }
}
