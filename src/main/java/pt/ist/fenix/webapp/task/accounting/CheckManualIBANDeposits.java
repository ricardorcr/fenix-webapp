package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class CheckManualIBANDeposits extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getIBANGroupSet().stream()
                .flatMap(iGroup -> iGroup.getIBANSet().stream())
                .flatMap(iban -> iban.getIBANPaymentSet().stream())
                .map(ibanPayment -> ibanPayment.getAccountingTransaction().getEvent())
                .forEach(event -> {
                    taskLog("Evento: %s%n", event.getExternalId());
                    event.getAccountingTransactionsSet().stream()
                            .filter(at -> !at.isAdjustingTransaction() && !at.hasBeenAdjusted())
                            .forEach(at -> taskLog("\t%s%n", at.getPaymentMethod().getDescription().getContent()));
                });
    }
}
