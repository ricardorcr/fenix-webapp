package pt.ist.fenix.webapp.servlet;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class CreateDebtCredit extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Event event = FenixFramework.getDomainObject("1976574014390690");

    }
}
