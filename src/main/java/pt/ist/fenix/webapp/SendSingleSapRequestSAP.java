package pt.ist.fenix.webapp;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class SendSingleSapRequestSAP extends SapCustomTask {

    @Override
    protected void runTask(final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        final SapRequest sapRequest = FenixFramework.getDomainObject("1134133360167004");
        sapRequest.setIntegrated(false);
        final SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
        sendSapRequest(sapEvent, sapRequest, errorLogConsumer, elogger);
    }

    private boolean sendSapRequest(SapEvent sapEvent, SapRequest sapRequest, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        try {
            return FenixFramework.atomic(() -> sapEvent.processPendingRequests(sapRequest, errorLogConsumer, elogger));
        } catch (Exception e) {
            taskLog("Exception for: %s %s%n", sapRequest.getEvent().getExternalId(), sapRequest.getDocumentNumber());
            return false;
        }
    }
}
