package bg.energo.phoenix.model.response.proxy;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileContent {
    private String fileName;
    private byte[] content;
}
