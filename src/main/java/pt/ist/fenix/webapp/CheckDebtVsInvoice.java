package pt.ist.fenix.webapp;

import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.SapEvent;

import java.util.AbstractMap;
import java.util.List;
import java.util.SortedMap;

public class CheckDebtVsInvoice extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(event -> !event.getSapRequestSet().isEmpty())
                .filter(event -> event.getSapRequestSet().stream().anyMatch(sr -> sr.getRequestType().equals(SapRequestType.DEBT)))
                .forEach(event -> {
                    final SapEvent sapEvent = new SapEvent(event);
//                    final SortedMap<SapRequest, Money> invoicesAndRemainingValue = sapEvent.getOpenInvoicesAndRemainingValue();
//                    final AbstractMap.SimpleImmutableEntry<List<SapRequest>, Money> debtsAndRemainingValue = sapEvent.getOpenDebtsAndRemainingValue();
//                    final Money invoiceValue = invoicesAndRemainingValue.values().stream().reduce(Money.ZERO, Money::add);
//                    Money debtValue = debtsAndRemainingValue.getValue();
//                    if (!invoiceValue.equals(debtValue)) {
//                        debtValue = debtValue.subtract(sapEvent.getPayedAmount());
//                        if (!invoiceValue.equals(debtValue)) {
//                            taskLog("%s\t%s\t%s%n", event.getExternalId(), debtValue.toString(), invoiceValue.toString());
//                        }
//                    }
                    final Money invoiceAmount = sapEvent.getInvoiceAmount();
                    final Money debtAmount = sapEvent.getDebtAmount();
                    final Money creditAmount = sapEvent.getFilteredSapRequestStream()
                            .filter(sr -> sr.getRequestType().equals(SapRequestType.CREDIT))
                            .filter(sr -> sr.getPayment() == null)
                            .map(SapRequest::getValue).reduce(Money.ZERO, Money::add);
                    final Money debtCreditAmount = sapEvent.getDebtCreditAmount();
                    final Money debtResult = debtAmount.subtract(debtCreditAmount);
                    final Money invoiceResult = invoiceAmount.subtract(creditAmount);
                    if (!debtResult.equals(invoiceResult)) {
                        final boolean sent2023 = event.getSapRequestSet().stream().anyMatch(sr -> sr.getWhenCreated().getYear() == 2023);
                        taskLog("%s\t%s\t%s\t%s\t%s\t%s%n", event.getExternalId(), sent2023, event.getWhenOccured().toString("dd-MM-yyyy"),
                                event.executionYearOf().getName(), debtResult.toString(), invoiceResult.toString());
                    }

                });
    }
}
