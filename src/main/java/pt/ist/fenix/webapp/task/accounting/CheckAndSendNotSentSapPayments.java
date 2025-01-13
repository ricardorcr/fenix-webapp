package pt.ist.fenix.webapp.task.accounting;

import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;

public class CheckAndSendNotSentSapPayments extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        //the 7 days offset was still on and automatic synchronization was stopped
        final int YEAR = 2024;
        try {
            SapRoot.getInstance().setOffsetDays(0);
            SapRoot.getInstance().getSapRequestSet().stream()
                    .filter(sr -> !sr.getSent())
                    .filter(sr -> !sr.getIntegrated())
                    .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT
                            || sr.getRequestType() == SapRequestType.ADVANCEMENT
                            || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                    .filter(sr -> sr.getDocumentDate().getYear() == YEAR)
                    .forEach(sr -> {
                        taskLog("%s\t%s\t%s%n", sr.getEvent().getExternalId(), sr.getDocumentNumber(), sr.getDocumentDate());
                        SapEvent sapEvent = new SapEvent(sr.getEvent());
                        sapEvent.processPendingRequests(sr.getEvent(), errorLogConsumer, elogger);
                    });
        } finally {
            SapRoot.getInstance().setOffsetDays(7);
        }
    }
}
