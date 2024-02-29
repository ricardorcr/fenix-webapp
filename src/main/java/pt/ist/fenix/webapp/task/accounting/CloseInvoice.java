package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

public class CloseInvoice extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapEvent sapEvent = new SapEvent(FenixFramework.getDomainObject("290082091173407"));
        sapEvent.closeDocument(FenixFramework.getDomainObject("289708429386288"));
    }
}
