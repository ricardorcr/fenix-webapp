package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.util.Money;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckSibsSapRequestsValue extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Map<AccountingTransaction, Money> values = new HashMap<>();
        Map<AccountingTransaction, List<SapRequest>> requests = new HashMap<>();
        Map<AccountingTransaction, Money> valuesTxRequests = new HashMap<>();
        LocalDate day = new LocalDate(2019,9,20);
        Money money = SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                //.filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT_INTEREST || sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(sr -> sr.getAnulledRequest() == null && sr.getOriginalRequest() == null)
                .filter(sr -> !isIgnoredAndTxAlreadySent(sr))
                .filter(sr -> sr.getRequest().contains("paymentMechanism\":\"SI"))
                .filter(sr -> {
                    if (sr.getOriginalRequest() != null) {
                        return sr.getOriginalRequest().getPayment() != null ? sr.getOriginalRequest().getPayment().getTransactionDetail() instanceof SibsTransactionDetail : false;
                    } else {
                        return sr.getPayment() != null ? sr.getPayment().getTransactionDetail() instanceof SibsTransactionDetail : false;
                    }
                })
                .filter(sr -> day.isEqual(getSibsDate(sr)))
                .peek(sr -> {
                    SapRequest request = sr.getOriginalRequest() != null ? sr.getOriginalRequest() : sr;
                    Money requestTotal = request.getValue().add(request.getAdvancement());

                    Money total = values.computeIfAbsent(request.getPayment(), (t) -> Money.ZERO).add(requestTotal);
                    values.put(request.getPayment(), total);

                    Money requestsValue = request.getPayment().getSapRequestSet().stream()
                            .filter(osr -> osr.getRequestType() != SapRequestType.INVOICE && osr.getRequestType() != SapRequestType.INVOICE_INTEREST)
                            .filter(osr -> osr.getOriginalRequest() == null && osr.getAnulledRequest() == null)
                            .map(osr -> osr.getValue().add(osr.getAdvancement()))
                            .reduce(Money.ZERO, Money::add);

                    Money totalDiffs = valuesTxRequests.computeIfAbsent(request.getPayment(), (t) -> Money.ZERO).add(requestsValue);
                    valuesTxRequests.put(sr.getPayment(), totalDiffs);

                    requests.computeIfAbsent(request.getPayment(), (t) -> new ArrayList<SapRequest>()).add(sr);
                })
                .map(sr -> sr.getValue().add(sr.getAdvancement()))
                .reduce(Money.ZERO, Money::add);

        taskLog("Valor para o dia %s: %s%n", day, money);

        final Money[] difference = new Money[] {Money.ZERO};
        values.forEach((at, m) -> {
            if (!at.getOriginalAmount().equals(m)) {
                taskLog("Evento: %s tx: %s %s - requests: %s%n", at.getEvent().getExternalId(), at.getExternalId(), at.getOriginalAmount(), m);
                difference[0] = difference[0].add(m.subtract(at.getOriginalAmount()));
            }
        });
        requests.forEach((at, list) -> {
            if (list.size() > 1) {
                taskLog("Olhar para evento: %s transacção: %s %s%n", at.getEvent().getExternalId(), at.getExternalId(), at.getOriginalAmount());
            }
        });
        valuesTxRequests.forEach((at, m) -> {
            if (!at.getOriginalAmount().equals(m)) {
                taskLog("Há diferenças no evento: %s tx: %s %s - requests:%s%n", at.getEvent().getExternalId(), at.getExternalId(), at.getOriginalAmount(), m);
            }
        });
        taskLog("Valor total da diferença: %s total/2: %s%n", difference[0], difference[0].divide(Money.valueOf(2)));
    }

    private LocalDate getSibsDate(final SapRequest sr) {
        SapRequest request = sr.getOriginalRequest() != null ? sr.getOriginalRequest() : sr;
        SibsTransactionDetail sibsTransactionDetail = (SibsTransactionDetail) request.getPayment().getTransactionDetail();
        SibsIncommingPaymentFileDetailLine sibsLine = sibsTransactionDetail.getSibsLine();
        if (sibsLine != null) {
            return sibsLine.getHeader().getWhenProcessedBySibs().toLocalDate();
        }
        return null;
    }

    private boolean isIgnoredAndTxAlreadySent(final SapRequest sapRequest) {
        if (sapRequest.getIgnore() && sapRequest.getPayment() != null
                && sapRequest.getAnulledRequest() == null && sapRequest.getOriginalRequest() == null) {
            final Money originalValue = sapRequest.getPayment().getOriginalAmount();
            final Money requestValue = sapRequest.getValue().add(sapRequest.getAdvancement());
            final Money totalOtherValue = sapRequest.getPayment().getSapRequestSet().stream()
                    .filter(osr -> sapRequest != osr)
                    .filter(osr -> isPayment(osr))
                    .filter(osr -> !osr.getIgnore())
                    .map(osr -> osr.getValue().add(osr.getAdvancement()))
                    .reduce(Money.ZERO, Money::add);
            return requestValue.add(totalOtherValue).greaterOrEqualThan(originalValue) &&
                    sapRequest.getPayment().getSapRequestSet().stream()
                            .filter(sr -> sr != sapRequest)
                            .filter(sr -> isPayment(sr))
                            .filter(sr -> !sr.getIgnore())
                            .anyMatch(sr -> {
                                Money srValue = sr.getValue().add(sr.getAdvancement());
                                return srValue.equals(requestValue);
                            });
        }
        return false;
    }

    private boolean isPayment(final SapRequest request) {
        final SapRequestType type = request.getRequestType();
        return type == SapRequestType.PAYMENT || type == SapRequestType.PAYMENT_INTEREST
                || type == SapRequestType.ADVANCEMENT;
    }
}
