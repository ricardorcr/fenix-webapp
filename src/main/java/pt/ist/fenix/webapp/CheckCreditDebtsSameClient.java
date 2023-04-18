package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class CheckCreditDebtsSameClient extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Dívidas Clientes diferentes");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> !sr.isInitialization())
                .filter(this::notSameClient)
                .forEach(sr -> report(sr, spreadsheet));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Abates_clientes_diferentes.xlsx", baos.toByteArray());
    }

    private void report(final SapRequest debtCredit, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        SapRequest debt = debtCredit.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(debtCredit.getDocumentNumberForType("NG")))
                .findAny().get();
        row.setCell("Cliente Dívida", debt.getClientData().getClientId());
        row.setCell("Dívida Doc", debt.getDocumentNumber());
        row.setCell("Cliente Abate", debtCredit.getClientData().getClientId());
        row.setCell("Abate Doc", debtCredit.getDocumentNumber());
        row.setCell("Valor abate", debtCredit.getValue().toString());
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + debtCredit.getEvent().getExternalId());
    }

    private boolean notSameClient(final SapRequest debtCredit) {
        final SapRequest debt = debtCredit.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(debtCredit.getDocumentNumberForType("NG")))
                .findAny().get();
        return getStartDate(debt).getYear() > 2018 && !debt.getClientData().getClientId().equals(debtCredit.getClientData().getClientId());
    }

    private LocalDate getStartDate(final SapRequest debt) {
        String metadata = debt.getRequestAsJson().get("workingDocument").getAsJsonObject().get("metadata").getAsString();

        String stripMetadata = metadata.replace("\\", "");
        JsonObject metadataJson = new JsonParser().parse(stripMetadata).getAsJsonObject();
        return LocalDate.parse(metadataJson.get("START_DATE").getAsString());
    }
}
