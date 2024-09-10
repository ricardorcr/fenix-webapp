package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.joda.time.DateTime;
import pt.ist.payments.domain.SibsPaymentMethod;
import pt.ist.payments.domain.SibsPaymentSystem;

public class CheckCardPayments extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final DateTime dateTime = new DateTime(2024, 7, 22, 0, 0);
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(txDetails -> txDetails.getPaymentMethod() == PaymentMethod.getCardPaymentMethod())
                .filter(txDetails -> txDetails.getWhenRegistered().isAfter(dateTime))
                .forEach(txDetails -> taskLog("%s\t%s\t%s\t%s%n", txDetails.getEvent().getExternalId(),
                        txDetails.getExternalId(), txDetails.getWhenRegistered(),
                        txDetails.getTransaction().getOriginalAmount().toString()));


        taskLog("##### Movimentos SIBS");
        SibsPaymentSystem.getInstance().getSibsPaymentSet().stream()
                .filter(payment -> payment.getMethod() == SibsPaymentMethod.CARD)
                .forEach(payment -> taskLog("%s\t%s\t%s\t%s%n", payment.getExternalId(), payment.getCreatedAt(),
                        payment.getStatus().name(), payment.getSettlement()));
    }
}
