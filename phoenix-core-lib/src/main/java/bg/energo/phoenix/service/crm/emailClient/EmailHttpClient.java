package bg.energo.phoenix.service.crm.emailClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

public class EmailHttpClient {

    public byte[] post(URI uri, Map<String, String> headers, byte[] data) throws IOException {
        URL url = uri.toURL();

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Add headers
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            // Send data
            try (OutputStream os = connection.getOutputStream()) {
                os.write(data);
            }

            // Get response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream is = connection.getInputStream()) {
                    return is.readAllBytes();
                }
            } else {
                throw new IOException("Failed : HTTP error code : " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
