package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.Event;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ChangeDocumentDatesTo2019 extends SapCustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        final EventLogger elogger2 = (msg, args) -> { };

        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.getIntegrated())
                .map(sr -> sr.getEvent())
                .distinct()
                .forEach(e -> process(e, errorLogConsumer, elogger2));
    }


    private static final Comparator<SapRequest> REQUEST_COMPARATOR = (sr1, sr2) ->
            SapRequest.COMPARATOR_BY_ORDER.thenComparing(SapRequest.DOCUMENT_NUMBER_COMPARATOR).compare(sr1, sr1);

    private void process(Event event, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        final List<SapRequest> requestToSend = event.getSapRequestSet().stream()
                .filter(sr -> !sr.getIntegrated())
                .sorted(REQUEST_COMPARATOR)
                .collect(Collectors.toList());
        for (final SapRequest sapRequest : requestToSend) {
            final SapRequestType sapRequestType = sapRequest.getRequestType();
            final DateTime documentDate;

            if (sapRequestType == SapRequestType.DEBT || sapRequestType == SapRequestType.DEBT_CREDIT) {
                documentDate = documentDateFor(sapRequest, "workingDocument", "documentDate");
            } else if (sapRequestType == SapRequestType.INVOICE || sapRequestType == SapRequestType.INVOICE_INTEREST) {
                documentDate = documentDateFor(sapRequest, "workingDocument", "documentDate");
            } else if (sapRequestType == SapRequestType.CREDIT) {
                documentDate = documentDateFor(sapRequest, "workingDocument", "documentDate");
            } else if (sapRequestType == SapRequestType.PAYMENT || sapRequestType == SapRequestType.PAYMENT_INTEREST
                    || sapRequestType == SapRequestType.ADVANCEMENT || sapRequestType == SapRequestType.CLOSE_INVOICE) {
                documentDate = documentDateFor(sapRequest, "paymentDocument", "paymentDate");
            } else if (sapRequestType == SapRequestType.REIMBURSEMENT) {
                documentDate = documentDateFor(sapRequest, "paymentDocument", "paymentDate");
            } else {
                throw new Error("unreachable code");
            }

            if (documentDate.getYear() < 2020) {
                if (sapRequestType == SapRequestType.REIMBURSEMENT) {
                    taskLog("Reembolso %s event: %s%n", sapRequest.getDocumentNumber(), event.getExternalId());
//                    hackDocumentDateDate(sapRequest, new DateTime());
                }
            }
        }
    }

    private static final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private void hackDocumentDateDate(final SapRequest sapRequest, final DateTime dt) {
        FenixFramework.atomic(() -> {
            final JsonObject request = sapRequest.getRequestAsJson();
            if (request.get("workingDocument") != null && !request.get("workingDocument").isJsonNull()) {
                final JsonObject workingDocument = request.get("workingDocument").getAsJsonObject();
                workingDocument.addProperty("documentDate", dt.toString(DT_FORMAT));
            }
            if (request.get("paymentDocument") != null && !request.get("paymentDocument").isJsonNull()) {
                final JsonObject paymentDocument = request.get("paymentDocument").getAsJsonObject();
                paymentDocument.addProperty("paymentDate", dt.toString(DT_FORMAT));
            }
            sapRequest.setRequest(request.toString());
        });
    }

    private DateTime documentDateFor(final SapRequest sr, final String workingDocument, final String documentDate) {
        try {
            final String s = sr.getRequestAsJson().get(workingDocument).getAsJsonObject().get(documentDate).getAsString();
            return new DateTime(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(5, 7)), Integer.parseInt(s.substring(8, 10)),
                    Integer.parseInt(s.substring(11, 13)), Integer.parseInt(s.substring(14, 15)), Integer.parseInt(s.substring(17, 19)));
        } catch (NullPointerException ex) {
            taskLog("NPE: %s %s %s %n", sr.getEvent().getExternalId(), sr.getDocumentNumber(), sr.getRequest());
            throw ex;
        }
    }
}