package pt.ist.fenix.webapp;

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

public class Send2019InvoicesToSap extends SapCustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    int count = 0;
    int error = 0;

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        final EventLogger elogger2 = (msg, args) -> { };

        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.getIntegrated())
                .map(sr -> sr.getEvent())
                .distinct()
                .forEach(e -> process(e, errorLogConsumer, elogger2));

        taskLog("Sent %s   Failed %s%n", count, error);
    }


    private static final Comparator<SapRequest> REQUEST_COMPARATOR = (sr1, sr2) ->
            SapRequest.COMPARATOR_BY_ORDER.thenComparing(SapRequest.DOCUMENT_NUMBER_COMPARATOR).compare(sr1, sr1);

    private void process(Event event, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        final List<SapRequest> requestToSend = event.getSapRequestSet().stream()
                .filter(sr -> !sr.getIntegrated())
                .sorted(REQUEST_COMPARATOR)
                .collect(Collectors.toList());
        final SapEvent sapEvent = new SapEvent(event);
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

            if (documentDate.getYear() < 2019) {
                taskLog("Late document for event: %s %s %s%n", event.getExternalId(), sapRequest.getDocumentNumber(), documentDate.getYear());
                return;
            }

            if (documentDate.getYear() == 2018) {
                if (sapRequestType == SapRequestType.ADVANCEMENT || sapRequestType == SapRequestType.PAYMENT || sapRequestType == SapRequestType.PAYMENT_INTEREST) {
                    taskLog("Late invoice for event: %s %s %s%n", event.getExternalId(), sapRequest.getDocumentNumber(), documentDate.getYear());
//                    if (!sendSapRequest(sapEvent, sapRequest, errorLogConsumer, elogger)) {
//                        error++;
//                        taskLog("Got an error processing: %s %s%n", event.getExternalId(), sapRequest.getDocumentNumber());
//                        return;
//                    }
                    count++;
                }
            }
        }

        final int sent = count + error;
        if (sent > 0 && (sent % 100) == 0) {
            taskLog("Sent %s   Failed %s%n", count, error);
        }
    }

    private boolean sendSapRequest(SapEvent sapEvent, SapRequest sapRequest, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        try {
            if (true) return false;
            return FenixFramework.atomic(() -> sapEvent.processPendingRequests(sapRequest, errorLogConsumer, elogger));
        } catch (Exception e) {
            taskLog("Exeption for: %s %s%n", sapRequest.getEvent().getExternalId(), sapRequest.getDocumentNumber());
            return false;
        }
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