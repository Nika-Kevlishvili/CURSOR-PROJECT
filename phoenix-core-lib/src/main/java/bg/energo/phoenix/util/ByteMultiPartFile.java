package bg.energo.phoenix.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class ByteMultiPartFile implements MultipartFile {


    private final String name;
    private final byte[] input;
    private String contentType;

    public ByteMultiPartFile(String name, byte[] input) {
        this.name = name;
        this.input = input;
    }

    public ByteMultiPartFile(String name, byte[] input, String contentType) {
        this.name = name;
        this.input = input;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return name;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
            return input == null || input.length == 0;
    }

    @Override
    public long getSize() {
        return input.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return input;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(input);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try(FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(input);
        }
    }
}
