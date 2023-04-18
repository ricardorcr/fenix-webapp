package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;

public class RegisterSapCredit extends SapCustomTask {

    @Override
    protected void runTask(final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        final Event event = FenixFramework.getDomainObject("1688875630071502");
        final SapEvent sapEvent = new SapEvent(event);

        sapEvent.registerCredit(event, getCreditEntry(new Money(5000)), false, false);
        sapEvent.registerCredit(event, getCreditEntry(new Money(2000)), false, false);
//        final SapRequest creditRequest = event.getSapRequestSet().stream()
//                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
//                .filter(sr -> sr.getWhenCreated().toLocalDate().equals(new LocalDate()))
//                .findAny().get();

//        final SapRequest creditRequest = new SapRequest(event, "", new Money(0.09), "NA0", SapRequestType.CREDIT, null, new JsonObject());
//        creditRequest.setIntegrated(true);
//        creditRequest.setSent(true);
//        creditRequest.setCreditId("852705627084127");
    }

    private CreditEntry getCreditEntry(final Money creditAmount) {
        return new CreditEntry("", new DateTime(), new LocalDate(), "", creditAmount.getAmount()) {

            @Override
            public BigDecimal getUsedAmountInDebts() {
                return getAmount();
            }

            @Override
            public boolean isToApplyInterest() {
                return false;
            }

            @Override
            public boolean isToApplyFine() {
                return false;
            }

            @Override
            public boolean isForInterest() {
                return false;
            }

            @Override
            public boolean isForFine() {
                return false;
            }

            @Override
            public boolean isForDebt() {
                return false;
            }
        };
    }
}
