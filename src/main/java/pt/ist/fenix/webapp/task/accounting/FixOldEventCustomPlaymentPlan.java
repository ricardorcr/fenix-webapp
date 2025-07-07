package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.events.EventExemption;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityExemptionJustificationByDispatch;
import org.fenixedu.academic.domain.accounting.events.gratuity.PercentageGratuityExemption;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashSet;
import java.util.Set;

public class FixOldEventCustomPlaymentPlan extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Event event = FenixFramework.getDomainObject("844450699936090");
        final PercentageGratuityExemption percentageExemption = event.getExemptionsSet().stream()
                .filter(PercentageGratuityExemption.class::isInstance)
                .map(PercentageGratuityExemption.class::cast)
                .findAny().get();

        GratuityExemptionJustificationByDispatch justificationMotive = (GratuityExemptionJustificationByDispatch) percentageExemption.getExemptionJustification();
        final EventExemption eventExemption = new EventExemption(event, percentageExemption.getResponsible(), new Money(478.56),
                percentageExemption.getExemptionJustification().getJustificationType(), justificationMotive.getGratuityExemptionDispatchDate().toDateTimeAtMidnight(),
                percentageExemption.getReason());
        eventExemption.setWhenCreated(percentageExemption.getWhenCreated());

        Set<Exemption> temp = new HashSet<>();
        event.getExemptionsSet().stream()
                .filter(exemption -> exemption != percentageExemption)
                        .forEach(exemption -> {
                            exemption.setEvent(null);
                            temp.add(exemption);
                        });

        Set<AccountingTransaction > transactions = new HashSet<>();
        transactions.addAll(event.getAccountingTransactionsSet());
        event.getAccountingTransactionsSet().forEach(transaction -> transaction.setEvent(null));

        Set<SapRequest> sapRequests = new HashSet<>();
        sapRequests.addAll(event.getSapRequestSet());
        sapRequests.forEach(sapRequest -> sapRequest.setEvent(null));

        percentageExemption.delete();
        temp.forEach(exemption -> exemption.setEvent(event));
        event.getSapRequestSet().addAll(sapRequests);
        event.getAccountingTransactionsSet().addAll(transactions);
    }
}
