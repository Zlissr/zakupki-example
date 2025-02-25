package rt.zakupki;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ArchiveDownloader {

    private final String token;

    public ArchiveDownloader(String token) {
        this.token = token;
    }

    public void downloadArchive(String archiveUrl) throws Exception {
        HttpsURLConnection connection = getConnection(archiveUrl);
        int responseCode = connection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Не удалось скачать архив. HTTP код: " + responseCode + " URL: " + archiveUrl);
        }

        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String fileName = "archive_" + timeStr + ".zip";
        File outputFile = new File(fileName);

        try (
                InputStream is = connection.getInputStream();
                OutputStream os = new FileOutputStream(outputFile)
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }

    private HttpsURLConnection getConnection(String archiveUrl) throws Exception {
        URL url = new URL(archiveUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("individualPerson_token", token);

        return conn;
    }
}
