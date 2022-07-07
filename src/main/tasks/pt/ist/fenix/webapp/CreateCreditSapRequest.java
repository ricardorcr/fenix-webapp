package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;

public class CreateCreditSapRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Event event = FenixFramework.getDomainObject("1407400653358341");
        final SapEvent sapEvent = new SapEvent(event);
        sapEvent.registerCredit(event, getCreditEntry(new Money(1063.47)), false, false);
    }

    CreditEntry getCreditEntry(final Money creditAmount) {
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