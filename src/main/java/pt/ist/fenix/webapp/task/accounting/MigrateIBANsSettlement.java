package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.academic.domain.accounting.IBANPayment;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.jwt.Tools;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MigrateIBANsSettlement extends ReadCustomTask {

    final String INPUT_DIRECTORY = "santander/pagamentos-iban-dedicado/processados";
    //    final String OUTPUT_NODE_ID = "851503036337306"; //"santander/pagamentos-iban-dedicado/processados";
    private final String accessToken = accessToken();

    @Override
    public void runTask() throws Exception {
        final JsonObject dir = readDirectory(INPUT_DIRECTORY);
        final Spreadsheet spreadsheet = new Spreadsheet("IBANs");
        for (final JsonElement e : dir.getAsJsonArray("items")) {
            final JsonObject item = e.getAsJsonObject();
            final String name = item.get("name").getAsString();
            final String downloadLink = item.get("downloadLink").getAsString();
            if (name.startsWith("2025") && name.endsWith(".txt")) {
                final String content = read(downloadLink);
                final List<String> allLines = Arrays.asList(content.replaceAll("\r", "").split("\n"));
                processFile(allLines, name, spreadsheet);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("migrated_ibans_settlements.xlsx", baos.toByteArray());
    }

    private void processFile(final List<String> content, final String name, final Spreadsheet spreadsheet) {
        FenixFramework.atomic(() -> {
            try {
                importIBANs(content, name, spreadsheet);
            } catch (Exception e) {
                throw new Error(e);
            }
        });
    }

    private JsonObject readDirectory(final String slug) {
        final HttpResponse<String> response = Unirest.get(getDriveUrl() + "/api/drive/directory/")
                .queryString("slug", slug)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Requested-With", "XMLHttpRequest")
                .asString();
        return new JsonParser().parse(response.getBody()).getAsJsonObject();
    }

    public String read(final String downloadLink) {
        HttpResponse<String> response = Unirest.get(downloadLink
                        //getDriveUrl() + "/api/drive/file/" + file.getContentKey() + "/download"
                )
                .header("Authorization", "Bearer " + accessToken)
                .asString();
        if (response.getStatus() == 307) {
            response = Unirest.get(response.getHeaders().getFirst("Location")).asString();
        }
        return response.getBody();
    }

    public String getDriveUrl() {
        final DriveAPIStorage driveAPIStorage = FileSupport.getInstance().getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().orElse(null);
        if (driveAPIStorage == null) {
            throw new Error("No DriveAPIStorage configured.");
        }
        return driveAPIStorage.getDriveUrl();
    }

    private String accessToken() {
        final JsonObject claim = new JsonObject();
        claim.addProperty("username", "fenix");
        return Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration().jwtPrivateKeyPath(), claim);
    }

    @Atomic
    private void importIBANs(final List<String> allLines, final String name, final Spreadsheet spreadsheet) {
        final Iterator<String> iter = allLines.iterator();
        String accountLine = null;
        int count = 0;
        final Map<LocalDate, Integer[]> dateCount = new HashMap<>();
        while (iter.hasNext()) {
            count++;
            final String line = iter.next();
            if (line.startsWith(":25:")) {
                accountLine = line;
                continue;
            }
            if (line.startsWith(":61:")) {
                count++;
                final String secondLine = iter.next();
                if (!secondLine.startsWith(":86:TRF REF:")) {
                    continue;
                }
                final int year = Integer.parseInt("20" + line.substring(4, 6));
                LocalDate settlementDate = new LocalDate(year, Integer.parseInt(line.substring(6, 8)), Integer.parseInt(line.substring(8, 10)));
                final Integer counter = ++dateCount.computeIfAbsent(settlementDate, v -> new Integer[]{0})[0];

                StringBuilder oldSettlement = new StringBuilder(String.valueOf(count)).append(" - ").append(accountLine);
                oldSettlement.append("\n").append(line);
                oldSettlement.append("\n").append(secondLine);

                StringBuilder newSettlement = new StringBuilder(String.valueOf(counter)).append(" - ").append(accountLine);
                newSettlement.append("\n").append(line);
                newSettlement.append("\n").append(secondLine);

                final IBANPayment ibanPayment = IBANPayment.lookup(oldSettlement.toString());
                if (ibanPayment != null) {
                    ibanPayment.setSettlement(newSettlement.toString());
                    ibanPayment.getAccountingTransaction().getTransactionDetail().setComments(newSettlement.toString());

                    Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("OID", ibanPayment.getExternalId());
                    row.setCell("File", name);
                    row.setCell("Old Settlement", oldSettlement.toString());
                    row.setCell("New Settlement", newSettlement.toString());
                }
            }
        }
    }
}
