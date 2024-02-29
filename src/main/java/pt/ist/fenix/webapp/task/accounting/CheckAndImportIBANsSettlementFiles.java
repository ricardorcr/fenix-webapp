package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.academic.domain.accounting.IBAN;
import org.fenixedu.academic.domain.accounting.IBANGroup;
import org.fenixedu.academic.domain.accounting.IBANPayment;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
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
import java.util.Iterator;
import java.util.List;

public class CheckAndImportIBANsSettlementFiles extends ReadCustomTask {

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
            if (name.endsWith(".txt")) {
                final String content = read(downloadLink);
                final List<String> allLines = Arrays.asList(content.replaceAll("\r", "").split("\n"));
                processFile(allLines, name, spreadsheet);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("ibans_importados.xlsx", baos.toByteArray());
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
        IBANGroup ibanGroup = null;
        final Iterator<String> iter = allLines.iterator();
        String accountLine = null;
        int count = 0;
        while (iter.hasNext()) {
            count++;
            final String line = iter.next();
            if (line.startsWith(":25:")) {
                accountLine = line;
                final String[] split = line.split(":25:");
                ibanGroup = Bennu.getInstance().getIBANGroupSet().stream()
                        .filter(group -> group.getAccount() == Long.parseLong(split[1]))
                        .findAny().get();
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
                LocalDate operationDate = new LocalDate(year, Integer.parseInt(line.substring(10, 12)), Integer.parseInt(line.substring(12, 14)));
                final int indexOf = line.indexOf("NMSCNONREF");
                final Money amount = new Money(line.substring(15, indexOf).replace(",", "."));

                final String[] parts = secondLine.split(":86:TRF REF:");
                final String reference = parts[1].trim().split(" ")[0];

                StringBuilder settlement = new StringBuilder(String.valueOf(count)).append(" - ").append(accountLine);
                settlement = settlement.append("\n").append(line);
                settlement = settlement.append("\n").append(secondLine);

                final IBANPayment lookup = IBANPayment.lookup(settlement.toString());
                if (lookup != null) {
                    continue;
                }

                final IBAN iban = ibanGroup.lookup(reference);
                if (iban == null) {
                    if (reference.startsWith("0")) {
                        taskLog("IBAN dedicado para esta conta fora do Fénix?");
                    }
                    continue;
                }

                final boolean hasPayment = iban.getEvent().getAccountingTransactionsSet().stream()
                        .filter(tx -> tx.getOriginalAmount().equals(amount))
                        .peek(tx -> taskLog("Já tem a tx: %s\t%s%nLinha: %s\tFicheiro: %s%n", tx.getExternalId(), tx.getEvent().getExternalId(), secondLine, name))
                        .findAny().isPresent();
                if (parts[1].startsWith(" ")) {
                    taskLog("Ficheiro: %s\t - %s%n", name, secondLine);
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Username", iban.getEvent().getPerson().getUsername());
                    row.setCell("Nome", iban.getEvent().getPerson().getName());
                    row.setCell("Evento", iban.getEvent().getExternalId());
                    row.setCell("Já pago", String.valueOf(hasPayment));
                    row.setCell("Ficheiro", name);
                    row.setCell("Linha", secondLine);
                    new IBANPayment(iban, operationDate, settlementDate, amount, settlement.toString());
                }
            }
        }
    }
}
