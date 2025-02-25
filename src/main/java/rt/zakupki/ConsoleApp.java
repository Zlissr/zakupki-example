package rt.zakupki;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsoleApp {

    private final SoapClient soapClient;
    private final ArchiveDownloader archiveDownloader;
    private final Scanner scanner;
    private final String token;

    public static void main(String[] args) {
        new ConsoleApp().run();
    }

    private ConsoleApp() {
        this.token = System.getenv("TOKEN");

        if (token == null || token.isBlank()) {
            System.err.println("Переменная окружения TOKEN не задана");
            System.exit(0);
        }

        archiveDownloader = new ArchiveDownloader(token);
        soapClient = new SoapClient(token);
        scanner = new Scanner(System.in);
    }

    private void run() {
        while (true) {


            System.out.print("Введите код региона: ");
            String regionInput = scanner.nextLine().trim();

            if (!isValidRegion(regionInput)) {
                System.err.println("Неверный ввод: ожидается число для региона.");
                continue;
            }

            System.out.print("Введите точную дату YYYY-MM-DD: ");
            String dateInput = scanner.nextLine().trim();

            if (!isValidDate(dateInput)) {
                System.err.println("Неверный формат даты. Ожидается формат YYYY-MM-DD.");
                continue;
            }

            try {
                String archiveUrl = soapClient.sendRequest
                        ("soap_getDocsByOrgRegionRequest.xml",
                                "RPZ223",
                                "purchasePlan",
                                regionInput,
                                dateInput
                        );

                archiveDownloader.downloadArchive(archiveUrl);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private boolean isValidRegion(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDate(String input) {
        try {
            LocalDate.parse(input);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private enum Actions {
        PURCHASE_PLAN,
        epNotificationEF2020

    }
}
