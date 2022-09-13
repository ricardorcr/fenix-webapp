package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

public class RegisterSapEvents extends SapCustomTask {

    @Override
    protected void runTask(final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        Arrays.asList("1688875630071488", "1688875630071487", "1688875630071485").forEach(oid -> {
                    final Event event = FenixFramework.getDomainObject(oid);
                    EventProcessor.registerEventSapRequests(errorLogConsumer, elogger, event, true);
                }
        );
    }
}
