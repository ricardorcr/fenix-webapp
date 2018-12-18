package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class CloseEvent extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Event event = FenixFramework.getDomainObject("1134150539018916");
        event.forceChangeState(EventState.CLOSED, new DateTime());
    }
}
