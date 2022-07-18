package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixframework.FenixFramework;

public class DebugEventStuff extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Event event = FenixFramework.getDomainObject("1978579764118023");
        final Person person = event.getPerson();
        ClientMap.uVATNumberFor(person);
    }
}