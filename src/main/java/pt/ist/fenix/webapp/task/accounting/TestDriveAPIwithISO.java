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
import org.fenixedu.jwt.Tools;
import pt.ist.fenixframework.FenixFramework;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TestDriveAPIwithISO extends ReadCustomTask {

    final String USERNAME = "fenix";
    final String INPUT_DIRECTORY = "santander/pagamentos-iban-dedicado/processados";
    final String accessToken = accessToken(USERNAME);

    @Override
    public void runTask() throws Exception {
        final JsonObject dir = readDirectory(INPUT_DIRECTORY);
        for (final JsonElement e : dir.getAsJsonArray("items")) {
            final JsonObject item = e.getAsJsonObject();
            final String name = item.get("name").getAsString();
            final String downloadLink = item.get("downloadLink").getAsString();
            if (name.startsWith("2025_02_25") && name.endsWith(".txt")) {
                final String content = read(downloadLink);
                final List<String> allLines = Arrays.asList(content.replaceAll("\r", "").split("\n"));
                processFile(allLines, name);
            }
        }

        final IBANPayment ibanPayment = FenixFramework.getDomainObject("290395623786287");
        taskLog("Ficou bem? %s%n", ibanPayment.getSettlement());
    }

    private void processFile(final List<String> content, final String fileName) {
        FenixFramework.atomic(() -> {
            try {
                writeSettlement(content, fileName);
            } catch (Exception e) {
                throw new Error(e);
            }
        });
    }

    private void writeSettlement(final List<String> allLines, final String fileName) {
        final Iterator<String> iter = allLines.iterator();
        String accountLine = null;
        final Map<String, Integer> lineCount = new HashMap<>();
        while (iter.hasNext()) {
            final String line = iter.next();
            if (line.startsWith(":25:")) {
                accountLine = line;
                continue;
            }
            if (line.startsWith(":61:")) {
                final String secondLine = iter.next();
                if (!secondLine.startsWith(":86:TRF REF:000021209 DE MËNICA DA CONCEIÃ")) {
                    continue;
                }
                final String txLine = new StringBuilder(accountLine)
                        .append("\n").append(line)
                        .append("\n").append(secondLine)
                        .toString();


                final Integer counter = lineCount.compute(txLine, (l, v) -> v == null ? 1 : (v + 1));
                final String settlement = new StringBuilder(String.valueOf(counter))
                        .append(" - ")
                        .append(txLine)
                        .toString();

                final IBANPayment ibanPayment = IBANPayment.lookup(settlement);
                taskLog(settlement);
                if (ibanPayment != null) {
                    taskLog("Encontrei");
                    if (ibanPayment.getExternalId().equals("290395623786287")) {
                        taskLog("Before: %s%nAfter: %s%n", ibanPayment.getSettlement(), settlement);
                        ibanPayment.setSettlement(settlement);
                        ibanPayment.getAccountingTransaction().getTransactionDetail().setComments(settlement);
                        break;
                    } else {
                        taskLog("Hummm %s%s", ibanPayment.getExternalId());
                    }
                }
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
        HttpResponse<byte[]> response = Unirest.get(downloadLink)
                .header("Authorization", "Bearer " + accessToken)
                .asBytes();
        if (response.getStatus() == 307) {
            response = Unirest.get(response.getHeaders().getFirst("Location")).asBytes();
        }
        return new String(response.getBody(), StandardCharsets.ISO_8859_1);
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
}
