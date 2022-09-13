package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReportAndSendMissingDebtCredit extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        final LocalDate createdDate = new LocalDate(2022,02,18);
//        final Spreadsheet spreadsheet = new Spreadsheet("Abates");
//        SapRoot.getInstance().getSapRequestSet().stream()
//                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
//                .filter(sr -> !sr.getIntegrated())
//                .filter(sr -> sr.getWhenCreated().toLocalDate().isEqual(createdDate))
//                .forEach(sr -> report(sr, spreadsheet));
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        try {
//            spreadsheet.exportToXLSXSheet(baos);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        output("abates_fora_prazo.xlsx", baos.toByteArray());

        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> !sr.getIntegrated())
                .filter(sr -> sr.getWhenCreated().toLocalDate().isEqual(createdDate))
                .forEach(sr -> send(sr, errorLogConsumer, elogger));
    }

    private void report(final SapRequest request, final Spreadsheet spreadsheet) {
        Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Nº Documento", request.getDocumentNumber());
        row.setCell("Referente a", request.getDocumentNumberForType("NG"));
        row.setCell("Valor", request.getValue().toString());
        row.setCell("Data Despacho", getDespachoDate(request));
        row.setCell("IstID", request.getEvent().getPerson().getUsername());
        row.setCell("Nome", request.getEvent().getPerson().getName());
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + request.getEvent().getExternalId());
    }

    private void send(final SapRequest debtCredit, final ErrorLogConsumer errorLog, final EventLogger elogger) {
        final SapEvent sapEvent = new SapEvent(debtCredit.getEvent());
        sapEvent.processPendingRequests(debtCredit, errorLog, elogger);
    }

    private String getDespachoDate(final SapRequest debtRequest) {
        final JsonObject workingDocument = debtRequest.getRequestAsJson().get("workingDocument").getAsJsonObject();
        final String metadataString = workingDocument.get("metadata").getAsString().replace("\\", "");
        final JsonObject metadata = new JsonParser().parse(metadataString).getAsJsonObject();
        return metadata.get("Despacho").getAsString();
    }
}
