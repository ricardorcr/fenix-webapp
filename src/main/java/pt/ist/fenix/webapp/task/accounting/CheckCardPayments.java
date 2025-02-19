package pt.ist.fenix.webapp.task.accounting;

import com.google.common.base.Strings;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.joda.time.DateTime;
import pt.ist.payments.domain.SibsPayment;
import pt.ist.payments.domain.SibsPaymentMethod;
import pt.ist.payments.domain.SibsPaymentSystem;

import java.util.Objects;

public class CheckCardPayments extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final DateTime dateTime = new DateTime(2025, 01, 01, 0, 0);
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(txDetails -> txDetails.getPaymentMethod() == PaymentMethod.getCardPaymentMethod())
                .filter(txDetails -> txDetails.getWhenRegistered().isAfter(dateTime))
                .forEach(txDetails -> taskLog("%s\t%s\t%s\t%s%n", txDetails.getEvent().getExternalId(),
                        txDetails.getExternalId(), txDetails.getWhenRegistered(),
                        txDetails.getTransaction().getOriginalAmount().toString()));


        taskLog("##### Movimentos SIBS");
        SibsPaymentSystem.getInstance().getSibsPaymentSet().stream()
                .filter(sibsPayment -> sibsPayment.getMethod() == SibsPaymentMethod.CARD)
                .filter(sibsPayment -> sibsPayment.getCreatedAt().isAfter(dateTime))
                .forEach(sibsPayment -> taskLog("%s\t%s\t%s\t%s\t%s%n", sibsPayment.getExternalId(),
                        sibsPayment.getCreatedAt(),
                        sibsPayment.getStatus().name(), getPaymentBrand(sibsPayment), sibsPayment.getSettlement()));
    }

    private String getPaymentBrand(final SibsPayment sibsPayment) {
        return sibsPayment.getLogSet().stream()
                .filter(log -> !Strings.isNullOrEmpty(log.getSibsResponse()))
                .map(log -> JsonParser.parseString(log.getSibsResponse()).getAsJsonObject())
                .filter(json -> json.has("paymentBrand"))
                .map(json -> json.get("paymentBrand").getAsString())
                .findAny()
                .orElse("");
    }
}
