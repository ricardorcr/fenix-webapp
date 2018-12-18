package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

public class RegisterAdvancementReimbursement extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapEvent sapEvent = new SapEvent(FenixFramework.getDomainObject("1977003511120340"));
        //sapEvent.registerReimbursementAdvancements();
    }
}
