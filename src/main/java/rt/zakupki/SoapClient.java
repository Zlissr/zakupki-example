package rt.zakupki;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SoapClient {

    private final String token;

    public SoapClient(String token) {
        this.token = token;
    }


    public String sendRequest(String template, String subsystemType, String documentType, String regionCode, String exactDate) throws Exception {
        String soapTemplate = loadTemplate(template);

        String uuid = UUID.randomUUID().toString();
        String createDateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String soapRequest = buildSoapRequest(soapTemplate, subsystemType, documentType, regionCode, uuid, createDateTime, exactDate);

        String serviceUrl = "https://int44.zakupki.gov.ru/eis-integration/services/getDocsIP";
        HttpsURLConnection conn = getConnection(serviceUrl);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = soapRequest.getBytes(StandardCharsets.UTF_8);
            os.write(input);
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode == HttpsURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream();
        String response = readStream(is);

        return parseArchiveUrl(response);
    }

    private HttpsURLConnection getConnection(String serviceUrl) throws Exception {
        URL url = new URL(serviceUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");

        return conn;
    }

    private String buildSoapRequest(String soapTemplate, String subsystemType, String documentType, String regionCode, String uuid, String createDateTime, String exactDate) {
        return String.format(soapTemplate, token, uuid, createDateTime, regionCode, subsystemType, documentType, exactDate);
    }

    private String loadTemplate(String resourceName) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                System.err.println("Ресурс " + resourceName + " не найден");
            }

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new Exception("Ошибка загрузки шаблона SOAP: " + e.getMessage());
        }
    }

    private String readStream(InputStream is) throws Exception {
        if (is == null) return "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }

            return sb.toString();
        }
    }

    private String parseArchiveUrl(String xml) throws Exception {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("archiveUrl");
            if (nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            throw new Exception("Ошибка при разборе XML: " + e.getMessage());
        }

        return null;
    }
}
