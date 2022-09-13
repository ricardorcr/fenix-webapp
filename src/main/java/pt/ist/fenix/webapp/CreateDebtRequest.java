package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.stream.Stream;

public class CreateDebtRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {

        final Event event = FenixFramework.getDomainObject("853032044595772");
        SapEvent sapEvent = new SapEvent(event);
        Method method = null;
        try {
            method = SapEvent.class.getDeclaredMethod("registerDebt", new Class[] {Money.class, Event.class, boolean.class});
            method.setAccessible(true);
            SapRequest sapRequest = (SapRequest) method.invoke(sapEvent, new Object[] {new Money(412.5), event, true});
            sapRequest.setRequest(sapRequest.getRequest().replace("2022-02-08", "2021-12-31"));
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
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