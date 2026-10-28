package com.shiv.resumeanalyzer.parsing;

import com.shiv.resumeanalyzer.web.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Extracts plain text from an uploaded resume PDF with Apache PDFBox. */
@Component
public class ResumeParser {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a resume PDF to upload.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The uploaded file couldn't be read.");
        }
        return extractText(bytes);
    }

    public String extractText(byte[] bytes) {
        // Check the file signature instead of trusting the file name or the browser's content type.
        if (bytes.length < PDF_MAGIC.length || !Arrays.equals(Arrays.copyOf(bytes, PDF_MAGIC.length), PDF_MAGIC)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The resume must be a PDF file.");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This PDF is password-protected. Upload an unlocked copy.");
            }
            String text = new PDFTextStripper().getText(document)
                    .replaceAll("[ \\t\\x0B\\f]+", " ")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();
            if (text.isBlank()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "No text was found in this PDF. If it's a scanned image, export your resume as a text PDF instead.");
            }
            return text;
        } catch (InvalidPasswordException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This PDF is password-protected. Upload an unlocked copy.");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This PDF couldn't be opened. It may be damaged.");
        }
    }
}
