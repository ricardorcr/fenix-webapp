package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.accounting.RefundState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

public class FixReimbursementState extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getSapRoot().getSapRequestSet().stream().parallel().forEach(sr -> process(sr));
    }

    private void process(final SapRequest sr) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {
                @Override
                public Void call() {
                    fixRefundState(sr);
                    return null;
                }
            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
        } catch (Exception e) {
            logError(sr, e);
            e.printStackTrace();
        }
    }

    private void fixRefundState(final SapRequest sr) {
        if (!sr.isInitialization() && sr.getRequestType() == SapRequestType.REIMBURSEMENT && RefundState.CONCLUDED == sr.getRefundState()) {
            Refund refund = sr.getRefund();
            if (refund.getState() != RefundState.CONCLUDED && isRefundComplete(refund)) {
                taskLog("Going to change state for: %s\t%s\t%s%n", refund.getExternalId(), sr.getDocumentNumber(), refund.getEvent().getExternalId());
                refund.setState(sr.getRefundState());
                refund.setStateDate(sr.getRefundStateDate());
            }
        }
    }

    private boolean isRefundComplete(final Refund refund) {
        return !refund.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.REIMBURSEMENT)
                .anyMatch(sr -> sr.getRefundState() != RefundState.CONCLUDED);
    }

    @Atomic(mode = TxMode.READ)
    private void logError(final SapRequest sr, final Exception e) {
        try {
            final String event = sr != null ? sr.getEvent() != null ? sr.getEvent().getExternalId() : "#####" : "$$$$$";
            taskLog("Error processing %s for event %s -> %s\n", sr.getExternalId(), event, e.getMessage());
        } catch (Exception ex) {
            taskLog("Erro no erro lol %s\t%s%n", sr.getExternalId(), ex.getMessage());
        }
    }
}
