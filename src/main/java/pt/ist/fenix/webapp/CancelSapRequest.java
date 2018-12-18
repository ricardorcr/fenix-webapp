package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

public class CancelSapRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRequest sapRequest = FenixFramework.getDomainObject("289708429034033");

        SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
        sapEvent.cancelDocument(sapRequest);

    }

}
