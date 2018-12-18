package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashSet;
import java.util.Set;

public class FixSapPayment extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        User responsible = User.findByUsername("ist24616");
        Event event = FenixFramework.getDomainObject("287724154127465");
        Set<Exemption> exemptions = new HashSet<>();
        exemptions.addAll(event.getExemptionsSet());

        event.getExemptionsSet().forEach(ex -> ex.setEvent(null));

        final SapRequest payment = event.getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equalsIgnoreCase("NP478579"))
                .findAny().get();

        final SapEvent sapEvent = new SapEvent(event);
        sapEvent.cancelDocument(payment);
        final SapRequest canceled = event.getSapRequestSet().stream()
                .filter(sr -> sr.getOriginalRequest() != null)
                .findAny().get();
        sapEvent.processPendingRequests(canceled, errorLogConsumer, elogger);

        event.getAccountingTransactionsSet().forEach(at -> at.annul(responsible, "Pagamento registado na pessoa errada."));

        exemptions.forEach(ex -> ex.setEvent(event));
    }
}
