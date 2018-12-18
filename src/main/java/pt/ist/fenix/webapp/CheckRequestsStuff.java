package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

public class CheckRequestsStuff extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream().parallel()
                .forEach(e -> check(e));
    }

    private void check(final Event event) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {
                @Override
                public Void call() {
                    if (!event.getSapRequestSet().isEmpty()) {
                        SapEvent sapEvent = new SapEvent(event);
                        if (sapEvent.getDebtCreditAmount().greaterThan(sapEvent.getDebtAmount())) {
                            taskLog("Debt Credit > Debt: %s%n", event.getExternalId());
                        }
                        if (sapEvent.getDebtAmount().isPositive()) {
                            if (sapEvent.getCreditAmount().greaterThan(sapEvent.getDebtAmount())) {
                                taskLog("Credit > Debt: %s%n", event.getExternalId());
                            }
                        }
                        if (sapEvent.getDebtCreditAmount().greaterThan(sapEvent.getCreditAmount())) {
                            taskLog("Debt Credit > Credit: %s%n", event.getExternalId());
                        }
                    }
                    return null;
                }
            }, new AtomicInstance(TxMode.READ, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
