package pt.ist.fenix.webapp;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class SendRequestsToSap extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        List<String> documentNumbers = null;
            documentNumbers = Arrays.asList("NP903716", "NR903718");
        for (String documentNumber : documentNumbers) {
            send(documentNumber, errorLogConsumer, elogger);
        }
    }

    private void send(final String documentNumber, final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        FenixFramework.atomic(() -> {
            final SapRequest sapRequest = SapRoot.getInstance().getSapRequestSet().stream()
                    .filter(sr -> sr.getDocumentNumber().equals(documentNumber))
                    .findAny().get();
            final SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
            sapEvent.processPendingRequests(sapRequest, errorLogConsumer, elogger);
        });
    }
}