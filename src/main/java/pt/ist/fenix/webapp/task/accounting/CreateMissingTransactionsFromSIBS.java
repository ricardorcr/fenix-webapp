package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.payments.domain.SibsPayment;
import pt.ist.payments.domain.SibsPaymentProgressStatus;
import pt.ist.payments.domain.SibsPaymentSystem;

public class CreateMissingTransactionsFromSIBS extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        SibsPaymentSystem.getInstance().getSibsPaymentSet().stream()
                .filter(sibsPayment -> sibsPayment.getStatus() == SibsPaymentProgressStatus.SUCCESSFUL)
                .filter(sibsPayment -> sibsPayment.getAccountingTransaction() == null)
                .filter(sibsPayment -> sibsPayment.getEvent() != null)
                .forEach(this::fix);
    }

    private void fix(SibsPayment sibsPayment) {
        FenixFramework.atomic(() -> {
            Signal.emit(SibsPayment.SUCCESSFUL_PAYMENT, new DomainObjectEvent<>(sibsPayment));
        });
    }
}
