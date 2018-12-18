package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.accounting.RefundState;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.ExcessRefund;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.FenixFramework;

public class FixReimbursementRefundConnection extends CustomTask {

    final DateTime now = new DateTime();

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getSapRoot().getSapRequestSet().stream().filter(sr -> !sr.getIgnore()
                && !sr.isInitialization() && sr.getRequestType() == SapRequestType.REIMBURSEMENT
                && sr.getRefund() == null).forEach(sr -> fix(sr));
    }

    private void fix(final SapRequest sr) {
        final DebtInterestCalculator calculator = sr.getEvent().getDebtInterestCalculator(now);
        final ExcessRefund refund = calculator.getExcessRefundStream().filter(r -> isItTheOne(r, sr)).findAny().orElse(null);
        if (refund != null) {
            taskLog("O SR: %s %s não tinha refund e vai passar a ter: %s%n", sr.getExternalId(), sr.getEvent().getExternalId(), refund.getId());
            final Refund refundDomain = (Refund) FenixFramework.getDomainObject(refund.getId());
            refundDomain.setState(RefundState.PENDING);
            refundDomain.setStateDate(new LocalDate());
            sr.setRefund(refundDomain);
        }
    }

    private boolean isItTheOne(final ExcessRefund refund, final SapRequest sr) {
        return refund.getPartialPayments().stream()
                .anyMatch(p -> p.getCreditEntry().getId().equals(sr.getAdvancementRequest().getPayment().getExternalId()));
    }
}
