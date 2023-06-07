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
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.jwt.Tools;
import pt.ist.fenixframework.Atomic;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FixSettlementInfoIbanPayments extends CustomTask {

    final String USERNAME = "fenix";
    final String INPUT_DIRECTORY = "santander/pagamentos-iban-dedicado/processados";

    private String accessToken = accessToken(USERNAME);

    @Override
    public void runTask() throws Exception {
        final JsonObject dir = readDirectory(INPUT_DIRECTORY);
        for (final JsonElement e : dir.getAsJsonArray("items")) {
            final JsonObject item = e.getAsJsonObject();
            final String name = item.get("name").getAsString();
            final String downloadLink = item.get("downloadLink").getAsString();
            if (name.endsWith(".txt")) {
                final String content = read(downloadLink);
                final List<String> allLines = Arrays.asList(content.replaceAll("\r", "").split("\n"));
                fixSettlementInfo(allLines);
            }
        }
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

    private String accessToken(final String username) {
        final JsonObject claim = new JsonObject();
        claim.addProperty("username", username);
        return Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration().jwtPrivateKeyPath(), claim);
    }

    @Atomic
    private void fixSettlementInfo(final List<String> allLines) {
        final Iterator<String> iter = allLines.iterator();
        String accountLine = null;
        int count = 0;
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

                StringBuilder settlement = new StringBuilder(accountLine);
                settlement = settlement.append("\n").append(line);
                settlement = settlement.append("\n").append(secondLine);

                final String suffix = count + " - ";
                final IBANPayment ibanPayment = IBANPayment.lookup(settlement.toString());
                if (ibanPayment != null) {
                    final String newSettlement = suffix + ibanPayment.getSettlement();
                    ibanPayment.setSettlement(newSettlement);
                    ibanPayment.getAccountingTransaction().getTransactionDetail().setComments(newSettlement);
                    taskLog("Changed payment: %s \t for event %s%n", ibanPayment.getAccountingTransaction().getExternalId(),
                            ibanPayment.getAccountingTransaction().getEvent().getExternalId());
                }
            }
        }
    }
}
