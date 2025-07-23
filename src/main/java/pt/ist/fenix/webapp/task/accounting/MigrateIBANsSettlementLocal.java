package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.IBANPayment;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MigrateIBANsSettlementLocal extends ReadCustomTask {

    final String INPUT_DIRECTORY = "/home/rcro/Documents/fenix/pagamentos/ibans/new";

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("IBANs");
        final File input = new File(INPUT_DIRECTORY);

        if (input.exists()) {
            for (final File file : Objects.requireNonNull(input.listFiles())) {
                if (file.getName().endsWith(".txt")) {
                    List<String> lines = Files.readAllLines(file.toPath());
                    processFile(lines, file.getName(), spreadsheet);
                }
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
