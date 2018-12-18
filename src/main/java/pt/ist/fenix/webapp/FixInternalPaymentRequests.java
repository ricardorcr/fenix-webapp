package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.FenixFramework;

public class FixInternalPaymentRequests extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream().parallel().forEach(e -> process(e));
    }

    private void process(final Event event) {
        FenixFramework.atomic(() -> {
            if (!event.getSapRequestSet().isEmpty()) {
                event.getAdjustedTransactions().stream()
                        .filter(tx -> tx.getTransactionDetail().getPaymentMethod().getInternalBennu() != null)
                        .forEach(tx -> fix(tx));
            }
        });
    }

    private void fix(final AccountingTransaction tx) {
        tx.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT && tx.getExternalId().equals(sr.getCreditId()))
                .forEach(sr -> {
                            taskLog("Request: %s Event: %s Payment: %s Credit: %s\n",
                                    sr.getExternalId(), tx.getEvent().getExternalId(), tx.getExternalId(), sr.getCreditId());
                            sr.setPayment(tx);
                        }
                );
    }
}
