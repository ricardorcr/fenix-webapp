package pt.ist.fenix.webapp;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

public class CorrectSAPInterestRequests extends CustomTask {

    final DateTime now = new DateTime();

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream().filter(e -> !e.getSapRequestSet().isEmpty()).forEach(this::process);

    }

    private void process(final Event event) {
        event.getSapRequestSet().stream().filter(sr -> sr.getRequestType().equals(SapRequestType.INVOICE_INTEREST))
                .forEach(this::process);
    }

    private void process(final SapRequest sapRequest) {
        DebtInterestCalculator debtInterestCalculator = sapRequest.getEvent().getDebtInterestCalculator(now);
        Optional<Payment> paymentById = debtInterestCalculator.getPaymentById(sapRequest.getPayment().getExternalId());
        if (!paymentById.isPresent()) {
            taskLog("Hold the horses!! O evento %s não tem o pagamento %s\n", sapRequest.getEvent().getExternalId(),
                    sapRequest.getPayment().getExternalId());
        } else {
            Payment payment = paymentById.get();
            Money interests = new Money(payment.getUsedAmountInFines().add(payment.getUsedAmountInInterests()));
            if (!interests.equals(sapRequest.getValue())) {
                Stream<SapRequest> requestToCorrect = sapRequest.getEvent().getSapRequestSet().stream()
                        .filter(sr -> sr.getPayment() != null).filter(sr -> sr.getPayment() == sapRequest.getPayment());
                correct(requestToCorrect, payment);
            }
        }
    }

    private void correct(Stream<SapRequest> requests, final Payment payment) {
        final boolean deleteInterest = payment.getAmount().compareTo(payment.getUsedAmountInDebts()) == 0;

        for (SapRequest sr : requests.collect(Collectors.toSet()))
        /*requests.forEach(sr -> */{
            if (sr.getRequestType().equals(SapRequestType.INVOICE)) {
                sr.setValue(new Money(payment.getUsedAmountInDebts()));
            } else if (sr.getRequestType().equals(SapRequestType.PAYMENT)) {
                sr.setValue(new Money(payment.getUsedAmountInDebts()));
            } else if (sr.getRequestType().equals(SapRequestType.INVOICE_INTEREST)) {
                if (deleteInterest) {
                    sr.delete();
                } else {
                    sr.setValue(new Money(payment.getUsedAmountInFines().add(payment.getUsedAmountInInterests())));
                }
            } else if (sr.getRequestType().equals(SapRequestType.PAYMENT_INTEREST)) {
                if (deleteInterest) {
                    sr.delete();
                } else {
                    sr.setValue(new Money(payment.getUsedAmountInFines().add(payment.getUsedAmountInInterests())));
                }
            } else if (sr.getRequestType().equals(SapRequestType.ADVANCEMENT)) {
                if (payment.getUnusedAmount().compareTo(BigDecimal.ZERO) == 0) {
                    sr.delete();
                } else {
                    //sr.setAdvancement(new Money(payment.getUnusedAmount()));
                    sr.setValue(null);
                }
            } else {
//                taskLog("Foneix!!");
                taskLog("O filtro para o evento %s não está a funcionar bem!! type: %s - Id: %s\n", sr.getEvent().getExternalId(),
                        sr.getRequestType(), sr.getExternalId());
                throw new Error("BOOM!!");
            }
        }//);
    }
}
