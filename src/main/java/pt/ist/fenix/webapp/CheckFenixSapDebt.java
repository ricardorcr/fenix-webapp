package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

public class CheckFenixSapDebt extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream()
            .filter(AdministrativeOfficeFeeEvent.class::isInstance)
            .filter(e -> !e.getSapRequestSet().isEmpty())
            .forEach(e -> {
                Money originalDebt = e.getOriginalAmountToPay();
                Money invoiceAmount = e.getSapRequestSet().stream()
                    .filter(sr -> !sr.getIgnore())
                    .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                    .map(SapRequest::getValue)
                    .reduce(Money.ZERO,Money::add);
                if (!originalDebt.equals(invoiceAmount)) {
                    taskLog("Check event: %s %s - valor original: %s - valor facturas: %s\n", e.getExternalId(), e.getWhenOccured(), originalDebt, invoiceAmount);
                }
            });
        
    }

}
