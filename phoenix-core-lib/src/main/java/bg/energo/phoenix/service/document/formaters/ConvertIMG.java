package bg.energo.phoenix.service.document.formaters;

import hr.ngs.templater.DocumentFactoryBuilder;
import hr.ngs.templater.ImageInfo;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
public class ConvertIMG implements DocumentFactoryBuilder.Formatter {
    @Override
    public Object format(Object value, String metadata) {
        if (metadata == null || value == null) {
            return value;
        }

        if (metadata.startsWith("html-img-")) {
            return formatHtmlImage(value, metadata);
        } else if (metadata.startsWith("img-")) {
            return formatImage(value, metadata);
        }

        return value;
    }

    private Object formatHtmlImage(Object value, String metadata) {
        try {
            String[] data = metadata.split("-");
            if (data.length != 4) {
                return value;
            }

            byte[] casedValue = (byte[]) value;
            int width = Integer.parseInt(data[2]);
            int height = Integer.parseInt(data[3]);

            BufferedImage originalImage = getBufferedImage(casedValue);
            if (originalImage == null) {
                return value;
            }

            Image resizedImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage bufferedResizedImage = createBufferedImage(resizedImage, width, height);
            byte[] resizedImageBytes = convertToByteArray(bufferedResizedImage);

            String base64Image = Base64.getEncoder().encodeToString(resizedImageBytes);
            return "<img src=\"data:image/png;base64," + base64Image + "\" />";

        } catch (Exception e) {
            log.error("Error formatting HTML image: {}", e.getMessage(), e);
            return value;
        }
    }

    private Object formatImage(Object value, String metadata) {
        try {
            String[] data = metadata.split("-");
            if (data.length != 3) {
                return value;
            }

            byte[] casedValue = (byte[]) value;
            int width = Integer.parseInt(data[1]);
            int height = Integer.parseInt(data[2]);

            return ImageInfo
                    .from(casedValue)
                    .width(width)
                    .height(height)
                    .extension("png")
                    .build();

        } catch (Exception e) {
            log.error("Error formatting image: {}", e.getMessage(), e);
            return value;
        }
    }

    private BufferedImage getBufferedImage(byte[] imageBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
        return ImageIO.read(byteArrayInputStream);
    }

    private BufferedImage createBufferedImage(Image resizedImage, int width, int height) {
        BufferedImage bufferedResizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bufferedResizedImage.getGraphics().drawImage(resizedImage, 0, 0, null);
        return bufferedResizedImage;
    }

    private byte[] convertToByteArray(BufferedImage bufferedResizedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedResizedImage, "png", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
