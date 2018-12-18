package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

import java.util.Optional;

public class CheckPaymentsHaveSapRequestAndEqualValue extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final DateTime now = new DateTime();
        Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(tx -> tx.getWhenRegistered().getYear() == 2019)
                .filter(tx -> tx.getAdjustedTransaction() == null && tx.getAdjustmentTransactionsSet().isEmpty())
                .filter(tx -> !tx.getSapRequestSet().isEmpty())
                .filter(tx -> tx.getRefund() == null || (tx.getRefund() != null && tx.getEvent() == tx.getRefund().getEvent()))
                .forEach(tx -> {
                    Money value = tx.getSapRequestSet().stream().filter(sr -> !sr.getIgnore() && sr.getOriginalRequest() == null)
                            .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.ADVANCEMENT
                                    || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST || sr.getRequestType() == SapRequestType.CREDIT)
                            .filter(this::isNotAdvancementUse)
                            .map(sr -> sr.getValue().add(sr.getAdvancement()))
                            .reduce(Money.ZERO, Money::add);
                    if (!value.equals(tx.getAmountWithAdjustment())) {
                        DebtInterestCalculator calculator = tx.getEvent().getDebtInterestCalculator(now);
                        Payment payment = calculator.getPaymentById(tx.getExternalId()).get();
                        payment.getUsedAmountInDebts();
                        payment.getUsedAmountInInterests();
                        payment.getUsedAmountInExcessRefunds();
                        taskLog("Falta registar valor da tx: %s para o evento: %s - valor tx: %s valor sapRequest: %s%n",
                                tx.getExternalId(), tx.getEvent().getExternalId(), tx.getAmountWithAdjustment(), value);
                    }
                });
    }

    private boolean isNotAdvancementUse(final SapRequest sapRequest) {
        if (sapRequest.getRequestType() == SapRequestType.PAYMENT || sapRequest.getRequestType() == SapRequestType.PAYMENT_INTEREST) {
            JsonObject requestAsJson = sapRequest.getRequestAsJson();
            JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
            if (paymentDocument.has("excessPayment") && paymentDocument.has("isToCreditTotal")
                    && paymentDocument.has("isAdvancedPayment") && paymentDocument.has("originatingOnDocumentNumber")) {
                return false;
            }
        }
        return true;
    }
}