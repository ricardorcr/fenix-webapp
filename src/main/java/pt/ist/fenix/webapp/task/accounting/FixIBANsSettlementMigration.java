package pt.ist.fenix.webapp.task.accounting;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.academic.domain.accounting.IBANPayment;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.jwt.Tools;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class FixIBANsSettlementMigration extends ReadCustomTask {

    final String USERNAME = "fenix";
    final String INPUT_DIRECTORY = "santander/pagamentos-iban-dedicado/processados";
    final String accessToken = accessToken(USERNAME);

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Fixed ibans");
        final Map<String, Set<IBANPayment>> ibanPaymentsMap = new HashMap<>();
        final Set<IBANPayment> fixedIbans = new HashSet<>();
        Bennu.getInstance().getIBANGroupSet().stream()
                .flatMap(ibanGroup -> ibanGroup.getIBANSet().stream())
                .flatMap(iban -> iban.getIBANPaymentSet().stream())
                .filter(ibanPayment -> ibanPayment.getSettlementDate().getYear() == 2025)
                .forEach(ibanPayment -> {
                    final String settlement = ibanPayment.getSettlement();
                    final int i = settlement.indexOf(" - ");
                    final String line = settlement.substring(i + 3);

                    ibanPaymentsMap.computeIfAbsent(line,
                                    d -> new TreeSet<>(
                                            Comparator.comparing((IBANPayment payment) -> payment.getAccountingTransaction().getWhenProcessed())
                                                    .thenComparing(IBANPayment::getExternalId)))
                            .add(ibanPayment);
                });

        final JsonObject dir = readDirectory(INPUT_DIRECTORY);
        for (final JsonElement e : dir.getAsJsonArray("items")) {
            final JsonObject item = e.getAsJsonObject();
            final String name = item.get("name").getAsString();
            final String downloadLink = item.get("downloadLink").getAsString();
            if (name.startsWith("2025") && name.endsWith(".txt")) {
                final String content = read(downloadLink);
                final List<String> allLines = Arrays.asList(content.replaceAll("\r", "").split("\n"));
                processFile(allLines, name, ibanPaymentsMap, spreadsheet, fixedIbans);
            }
        }

        final Spreadsheet toFix = spreadsheet.addSpreadsheet("To fix");
        Set<IBANPayment> ibanPaymentSet =
                ibanPaymentsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Sets.SetView<IBANPayment> difference = Sets.difference(ibanPaymentSet, fixedIbans);
        difference.forEach(ibanPayment ->
                FenixFramework.atomic(() -> {
                    final String settlement = ibanPayment.getSettlement();
                    ibanPayment.setSettlement("XXX " + settlement);
                    ibanPayment.getAccountingTransaction().getTransactionDetail().setComments("XXX " + settlement);
                    Spreadsheet.Row row = toFix.addRow();
                    row.setCell("OID", ibanPayment.getExternalId());
                    row.setCell("Event", ibanPayment.getAccountingTransaction().getEvent().getExternalId());
                    row.setCell("Tx ID", ibanPayment.getAccountingTransaction().getExternalId());
                    row.setCell("Settlement", ibanPayment.getSettlement());
                    row.setCell("Date", ibanPayment.getSettlementDate().toString("dd/MM/yyyy"));
                })
        );

        output("fixed_ibans_settlement.xlsx", spreadsheet.exportToXLSXSheet());
    }

    private void processFile(final List<String> content, final String fileName,
                             final Map<String, Set<IBANPayment>> ibanPaymentsMap,
                             final Spreadsheet spreadsheet, final Set<IBANPayment> fixedIbans) {
        FenixFramework.atomic(() -> {
            try {
                fixSettlement(content, fileName, ibanPaymentsMap, spreadsheet, fixedIbans);
            } catch (Exception e) {
                throw new Error(e);
            }
        });
    }

    private void fixSettlement(final List<String> allLines, final String fileName,
                               final Map<String, Set<IBANPayment>> ibanPaymentsMap,
                               final Spreadsheet spreadsheet, final Set<IBANPayment> fixedIbans) {
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
                if (!secondLine.startsWith(":86:TRF REF:")) {
                    continue;
                }
                final String txLine = new StringBuilder(accountLine)
                        .append("\n").append(line)
                        .append("\n").append(secondLine)
                        .toString();

                Set<IBANPayment> ibanPayments = ibanPaymentsMap.get(txLine);
                if (ibanPayments == null || ibanPayments.isEmpty()) {
                    taskLog("Não tem ibanPayment: %s\n%s%n%n", fileName, txLine);
                } else {
                    final Integer counter = lineCount.compute(txLine, (l, v) -> v == null ? 1 : (v + 1));
                    final String settlement = new StringBuilder(String.valueOf(counter))
                            .append(" - ")
                            .append(txLine)
                            .toString();

                    final IBANPayment ibanPayment = getPayment(ibanPayments, counter - 1);
                    if (ibanPayment == null) {
                        taskLog("There should be a payment for: %s %s %s\n%s%n", counter, ibanPayments.size(),
                                fileName, txLine);
                    } else if (!fixedIbans.contains(ibanPayment)) {
                        Spreadsheet.Row row = spreadsheet.addRow();
                        row.setCell("OID", ibanPayment.getExternalId());
                        row.setCell("Old Settlement", ibanPayment.getSettlement());
                        row.setCell("New Settlement", settlement);
                        row.setCell("File", fileName);

                        ibanPayment.setSettlement(settlement);
                        ibanPayment.getAccountingTransaction().getTransactionDetail().setComments(settlement);
                        fixedIbans.add(ibanPayment);
                    }
                }
            }
        }
    }

    private IBANPayment getPayment(Set<IBANPayment> ibanPayments, int position) {
        Iterator<IBANPayment> iterator = ibanPayments.iterator();
        int iter = 0;
        while (iterator.hasNext()) {
            IBANPayment ibanPayment = iterator.next();
            if (iter == position) {
                return ibanPayment;
            } else {
                iter++;
            }
        }
        return null;
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
