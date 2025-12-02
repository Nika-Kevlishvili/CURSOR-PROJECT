package bg.energo.phoenix.model.response.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceFileContent {
    private String fileName;
    private byte[] content;
}
