package pt.ist.fenix.webapp;

import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class ExportInitialSapDocuments extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final LocalDate end2017 = new LocalDate(2017,12,31);
        final Spreadsheet spreadsheet = new Spreadsheet("SapInit");
        final Money[] total = new Money[] { Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO };
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getDocumentDate().toLocalDate().equals(end2017))
                .forEach(sr -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    final Money value = sr.getValue().add(sr.getAdvancement());
                    row.setCell("Document", sr.getDocumentNumber());
                    row.setCell("Document SAP", sr.getSapDocumentNumber());
                    row.setCell("Type", sr.getRequestType().name());
                    row.setCell("Value", value.toPlainString());
                    row.setCell("Ignored", sr.getIgnore() ? "Ignored" : "");
                    row.setCell("Integrated", sr.getIntegrated() ? "Integrated" : "");
                    row.setCell("Estornado", sr.getAnulledRequest() != null ? "Estornado" : "");
                    row.setCell("Estorno", sr.getOriginalRequest() != null ? "Estorno" : "");
                    row.setCell("Event", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + sr.getEvent().getExternalId());

                    if (sr.getOriginalRequest() == null) {
                        total[0] = total[0].add(value);
                        if (sr.getIgnore()) {
                            total[1] = total[1].add(value);
                        } else {
                            total[2] = total[2].add(value);
                        }
                        if (sr.getAnulledRequest() == null) {
                            total[3] = total[3].add(value);
                        } else {
                            total[4] = total[4].add(value);
                        }
                    }
                });

        taskLog("Total: %s%n", total[0].toPlainString());
        taskLog("Não Ignorado: %s%n", total[2].toPlainString());
        taskLog("Não Estornado: %s%n", total[3].toPlainString());
        taskLog("Ignorado: %s%n", total[1].toPlainString());
        taskLog("Estornado: %s%n", total[4].toPlainString());

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("sapinit.xls", stream.toByteArray());
    }
}