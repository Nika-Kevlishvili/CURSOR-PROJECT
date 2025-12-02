package bg.energo.phoenix.service.document.formaters;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import hr.ngs.templater.DocumentFactoryBuilder;
import hr.ngs.templater.ImageInfo;

import java.io.ByteArrayOutputStream;

public class ConvertQR implements DocumentFactoryBuilder.Formatter {
    @Override
    public Object format(Object value, String metadata) {
        if (metadata == null || !metadata.startsWith("qr-")) return value;
        try {
            String[] data = metadata.split("-");
            if (data.length != 3) {
                return value;
            }
            BitMatrix matrix = new MultiFormatWriter().encode(String.valueOf(value), BarcodeFormat.QR_CODE, Integer.parseInt(data[1]), Integer.parseInt(data[2]));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "png", os);
            return ImageInfo.from(os.toByteArray())
                    .width(Integer.parseInt(data[1])).height(Integer.parseInt(data[2]))
                    .extension("png").build();
        } catch (Exception e) {
            e.printStackTrace();
            return value;
        }
    }
}