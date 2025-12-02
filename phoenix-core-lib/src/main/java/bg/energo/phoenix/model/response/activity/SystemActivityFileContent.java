package bg.energo.phoenix.model.response.activity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemActivityFileContent {
    private String name;
    private byte[] content;
}
