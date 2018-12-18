package pt.ist.fenix.task;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class ReportOldDebts extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        final Spreadsheet sheet = new Spreadsheet("Dividas");
//        SapRoot.getInstance().getSapRequestSet().stream()
//                .parallel()
//                .map(this::toReportLine)
//                .filter(l -> l != null)
//                .forEach(l -> {
//                    final Row row = sheet.addRow();
//                    row.setCell("eventId", l[0]);
//                    row.setCell("event", l[1]);
//                    row.setCell("requestId", l[2]);
//                    row.setCell("documentNumber", l[3]);
//                    row.setCell("value", l[4]);
//                    row.setCell("documentDate", l[5]);
//                    row.setCell("entryDate", l[6]);
//                    row.setCell("dueDate", l[7]);
//                    row.setCell("startDate", l[8]);
//                    row.setCell("endDate", l[9]);
//                    row.setCell("cancel documents", l[10]);
//                });
//        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        sheet.exportToXLSSheet(stream);
//        output("dividas.xls", stream.toByteArray());

        SapRequest sapRequestToAnull = FenixFramework.getDomainObject("1134133359149058");
        cancelDocument(sapRequestToAnull.getEvent(), sapRequestToAnull);
    }

//    private String[] toReportLine(final SapRequest r) {
//        try {
//            return FenixFramework.getTransactionManager().withTransaction(() -> {
//                if (r.getRequestType() != SapRequestType.DEBT || r.getDocumentNumber().equals("NG0")) {
//                    return null;
//                }
//                final JsonObject json = new JsonParser().parse(r.getRequest()).getAsJsonObject();
//                final JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();
//                if (workingDocument == null || workingDocument.isJsonNull()) {
//                    throw new Error("No working document for " + r.getExternalId());
//                }
//                final JsonElement dm = workingDocument.get("debtMetadata");
//                if (dm == null || dm.isJsonNull()) {
//                    throw new Error("No debtMetadata for " + r.getExternalId());
//                }
//                final JsonObject debtMetadata = new JsonParser().parse(dm.getAsString()).getAsJsonObject();
//                final String startDate = debtMetadata.get("START_DATE").getAsString();
//                final int startYear = Integer.parseInt(startDate.substring(0, 4));
//                if (startYear >= 2018) {
//                    return null;
//                }
//                final Event event = r.getEvent();
//
//                taskLog("Processing: %s %s %s %s%n", event.getExternalId(), event.getPerson().getUsername(), r.getExternalId(), r.getDocumentNumber());
//
//                final Set<SapRequest> previous = new HashSet<>(event.getSapRequestSet());
////                cancelDocument(event, r);
//                final Set<SapRequest> newRequests = new HashSet<>(event.getSapRequestSet());
//                newRequests.removeAll(previous);
//
//                return new String[] {
//                        event.getExternalId(),
//                        event.getDescription().toString(),
//                        r.getExternalId(),
//                        r.getDocumentNumber(),
//                        r.getValue().toPlainString(),
//                        workingDocument.get("documentDate").getAsString(),
//                        workingDocument.get("entryDate").getAsString(),
//                        workingDocument.get("dueDate").getAsString(),
//                        startDate,
//                        debtMetadata.get("END_DATE").getAsString(),
//                        newRequests.stream().map(sr -> sr.getDocumentNumber()).reduce("", (s1, s2) -> s1 + " " + s2)
//                };
//            }, new AtomicInstance(TxMode.READ, false));
//        } catch (final Exception e) {
//            throw new Error(e);
//        }
//    }

    public static void cancelDocument(final Event event, final SapRequest sapRequest) {
        final SapRequestType requestType = sapRequest.getRequestType();
        if (requestType != SapRequestType.DEBT) {
            throw new Error("label.document.type.cannot.be.canceled");
        }
        event.getSapRequestSet().stream()
                .filter(r -> !r.getIgnore())
                .filter(r -> r != sapRequest && r.refersToDocument(sapRequest.getDocumentNumber()))
                .findAny().ifPresent(r -> {
            throw new Error("label.error.invoice.already.used");
        });

        JsonObject jsonAnnulled = new JsonParser().parse(sapRequest.getRequest()).getAsJsonObject();
        if (requestType == SapRequestType.INVOICE
                || requestType == SapRequestType.INVOICE_INTEREST
                || requestType == SapRequestType.CREDIT
                || requestType == SapRequestType.ADVANCEMENT
                || requestType == SapRequestType.DEBT) {
            final JsonObject workDocument = jsonAnnulled.get("workingDocument").getAsJsonObject();
            workDocument.addProperty("workStatus", "A");
            workDocument.addProperty("documentDate", new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
        }
        if (requestType == SapRequestType.PAYMENT
                || requestType == SapRequestType.PAYMENT_INTEREST
                || requestType == SapRequestType.CREDIT
                || requestType == SapRequestType.ADVANCEMENT) {
            final JsonObject workDocument = jsonAnnulled.get("paymentDocument").getAsJsonObject();
            workDocument.addProperty("paymentStatus", "A");
            workDocument.addProperty("paymentDate", new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
        }
        if (requestType == SapRequestType.CREDIT
            /* && getFilteredSapRequestStream().anyMatch(r -> r.getRequestType() == SapRequestType.DEBT) */) {
            throw new Error("unreachable code");
            //registerDebt(sapRequest.getValue(), event, true);
        }

        final SapRequest sapRequestAnnulled = new SapRequest(sapRequest.getEvent(), sapRequest.getClientId(), sapRequest.getValue(),
                sapRequest.getDocumentNumber(), sapRequest.getRequestType(), sapRequest.getAdvancement(), jsonAnnulled);
        sapRequest.setAnulledRequest(sapRequestAnnulled);
        sapRequestAnnulled.setIgnore(true);
    }

}