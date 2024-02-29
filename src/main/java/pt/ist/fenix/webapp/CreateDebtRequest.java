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

        final Event event = FenixFramework.getDomainObject("565260645826671");
        SapEvent sapEvent = new SapEvent(event);
        Method method = null;
        try {
            method = SapEvent.class.getDeclaredMethod("registerDebt", new Class[] {Money.class, Event.class, boolean.class});
            method.setAccessible(true);
            SapRequest sapRequest = (SapRequest) method.invoke(sapEvent, new Object[] {new Money(124.36), event, false});
            sapRequest.setRequest(sapRequest.getRequest().replace("2022-05-27", "2023-09-17"));
            sapRequest.setRequest(sapRequest.getRequest().replace("2022-09-14", "2023-12-31"));
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
    }
}