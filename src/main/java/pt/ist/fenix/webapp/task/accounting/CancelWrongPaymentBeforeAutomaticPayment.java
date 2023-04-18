package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CancelWrongPaymentBeforeAutomaticPayment extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Arrays.asList("1975341358779263", "1975341358779262", "1975341358778967").stream()
                .map(eventID -> (Event) FenixFramework.getDomainObject(eventID))
                .forEach(this::fix);
    }

    private void fix(final Event event) {
        List<AccountingTransaction> toKeep = new ArrayList<>();
        event.getAccountingTransactionsSet().stream()
                .filter(at -> at.getPaymentMethod() == PaymentMethod.getIBANPaymentMethod())
                .forEach(at -> {
                    toKeep.add(at);
                    at.setEvent(null);
                });
        event.getAccountingTransactionsSet().stream()
                .forEach(at -> at.annul(Authenticate.getUser(), "Pagamento IBAN dedicado registado manualmente erradamente"));
        toKeep.stream()
                .forEach(at -> at.setEvent(event));
    }
}
