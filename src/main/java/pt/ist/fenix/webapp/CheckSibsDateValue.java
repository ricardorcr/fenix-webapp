package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.util.Money;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.Atomic;

public class CheckSibsDateValue extends CustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    final YearMonthDay dateToCheck = new YearMonthDay(2018, 8, 1);

    @Override
    public void runTask() throws Exception {
        Money sibsDateValue = SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                //.filter(sr -> !sr.getIgnore())
                //.filter(sr -> sr.getOriginalRequest() == null && sr.getAnulledRequest() == null)
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST
                        || sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(this::hasSibsDate)
                .peek(sr -> taskLog(sr.getDocumentNumber()))
                .map(sr -> sr.getValue().add(sr.getAdvancement()))
                .reduce(Money.ZERO, Money::add);

        taskLog("Valor SIBS para dia %s - %s%n", dateToCheck, sibsDateValue);
    }

    private boolean hasSibsDate(final SapRequest sapRequest) {
        YearMonthDay sibsFileDate = getSIBSFileDate(sapRequest, sapRequest.getRequestAsJson().getAsJsonObject("paymentDocument"));
        return sibsFileDate != null && sibsFileDate.isEqual(dateToCheck);
    }

    private YearMonthDay getSIBSFileDate(final SapRequest request, final JsonObject paymentDocument) {
        try {
            if ("SI".equals(paymentDocument.get("paymentMechanism").getAsString())
                    && "N".equals(paymentDocument.get("paymentStatus").getAsString())) {
                if (request.getPayment() != null && request.getPayment().getTransactionDetail() instanceof SibsTransactionDetail) {
                    SibsTransactionDetail sibsTransactionDetail = (SibsTransactionDetail) request.getPayment().getTransactionDetail();
                    SibsIncommingPaymentFileDetailLine sibsLine = sibsTransactionDetail.getSibsLine();
                    if (sibsLine != null) {
                        return sibsLine.getHeader().getWhenProcessedBySibs();
                    }
                }
                return null;
            } else {
                return null;
            }
        } catch (Exception e) {
            taskLog("#####%s - %s%n", request.getDocumentNumber(), request.getExternalId());
            throw e;
        }
    }
}
