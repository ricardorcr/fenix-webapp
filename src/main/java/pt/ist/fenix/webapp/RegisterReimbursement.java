package pt.ist.fenix.webapp;

import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

public class RegisterReimbursement extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapEvent sapEvent = new SapEvent(FenixFramework.getDomainObject("1695528534409771"));
        //sapEvent.registerReimbursement(new Money(20));
    }
}
