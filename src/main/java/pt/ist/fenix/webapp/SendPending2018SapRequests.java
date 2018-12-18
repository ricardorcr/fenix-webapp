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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SendPending2018SapRequests extends SapCustomTask {

    private static final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    int count = 0;
    int error = 0;
    Set<String> invoiceNumbers = new HashSet<>();
    Set<String> remainingInvoices = new HashSet<>();

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/facturas_corrigidas_lote1.txt");
        try {
            invoiceNumbers.addAll(Files.readAllLines(file.toPath()));
            remainingInvoices.addAll(invoiceNumbers);
        } catch (IOException e) {
            e.printStackTrace();
            taskLog(e.getMessage());
        }

        final EventLogger elogger2 = (msg, args) -> {
        };
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.getIntegrated())
                .map(sr -> sr.getEvent())
                .distinct()
                .forEach(e -> process(e, errorLogConsumer, elogger2));

        taskLog("Sent %s   Failed %s%n", count, error);
    }

    private void process(Event event, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
//        if (remainingInvoices.size() == 0) {
//            return;
//        }
        if ((count + error) >= 50 && (count + error) % 50 == 0) {
            taskLog("Sent %s   Failed %s%n", count, error);
        }

        final List<SapRequest> requestToSend = event.getSapRequestSet().stream()
                .filter(sr -> !sr.getIntegrated())
                .sorted(SapRequest.DOCUMENT_NUMBER_COMPARATOR)
                .collect(Collectors.toList());
        final SapEvent sapEvent = new SapEvent(event);
        for (final SapRequest sapRequest : requestToSend) {
            final SapRequestType sapRequestType = sapRequest.getRequestType();
            if (sapRequest.getOriginalRequest() != null) {
                taskLog("Estorno por comunicar: %s %s%n", event.getExternalId(), sapRequest.getDocumentNumber());
                return;
            }
            if (sapRequestType == SapRequestType.CREDIT) {
                final DateTime dt = documentDateFor(sapRequest, "workingDocument", "documentDate");
                if (dt.getYear() == 2018) {
                    taskLog("Warning Credit not sent: %s %s%n", event.getExternalId(), sapRequest.getDocumentNumber());
                }
            } else if (sapRequestType == SapRequestType.INVOICE || sapRequestType == SapRequestType.INVOICE_INTEREST) {
                final DateTime dt = documentDateFor(sapRequest, "workingDocument", "documentDate");
                if (dt.getYear() == 2018) {
                    throw new Error("2018 Invoice " + event.getExternalId());
                }
            } else if (sapRequestType == SapRequestType.PAYMENT || sapRequestType == SapRequestType.PAYMENT_INTEREST
                    || sapRequestType == SapRequestType.ADVANCEMENT || sapRequestType == SapRequestType.CLOSE_INVOICE) {
                final int paymentYear = documentDateFor(sapRequest, "paymentDocument", "paymentDate").getYear();
                if (paymentYear == 2018) {
//                    if (count > 50) {
//                        return;
//                    }
//                    if (isInvoiceCorrected(sapRequest)) {
                    if (!sendSapRequest(sapEvent, sapRequest, errorLogConsumer, elogger)) {
                        error++;
//                        taskLog("Got an error processing: %s %s%n", event.getExternalId(), sapRequest.getDocumentNumber());
                        return;
                    }
                    count++;
//                    taskLog("Sent: %s %s%n", event.getExternalId(), sapRequest.getDocumentNumber());
//                    }
                }
            }
        }
    }

    private boolean isInvoiceCorrected(final SapRequest payment) {
        String invoiceNumber = payment.getDocumentNumberForType("ND");
        if (invoiceNumber == null) {
            taskLog("Request %s para evento %s não está associado a factura\n", payment.getDocumentNumber(), payment.getEvent().getExternalId());
            return false;
        } else {
            if (invoiceNumbers.contains(invoiceNumber)) {
                remainingInvoices.remove(invoiceNumber);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean sendSapRequest(SapEvent sapEvent, SapRequest sapRequest, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        try {
            return FenixFramework.atomic(() -> sapEvent.processPendingRequests(sapRequest, errorLogConsumer, elogger));
        } catch (Exception e) {
            taskLog("Exeption for: %s %s%n", sapRequest.getEvent().getExternalId(), sapRequest.getDocumentNumber());
            return false;
        }
    }

    private void hackDates(SapRequest sapRequest, DateTime edt) {
        final JsonObject jo = sapRequest.getRequestAsJson();
        final JsonObject workingDocument = jo.get("workingDocument").getAsJsonObject();
        workingDocument.addProperty("documentDate", edt.toString(DT_FORMAT));
        workingDocument.addProperty("dueDate", edt.toString(DT_FORMAT));
        sapRequest.setRequest(jo.toString());
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