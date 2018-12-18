package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Stream;

public class CorrectDebtValue extends CustomTask {

    @Override
    public void runTask() throws Exception {

        Stream.of("569199130837935","850674107548199","850674107548203","850674107548206","850674107548207","850674107548246","1132149084258834","1132149084258836","1132149084258839","1132149084258933",
                "1413624060969225","1976574014390586","1976574014390590","1976574014390690","1976574014390721")
                .map(s -> (Event) FenixFramework.getDomainObject(s))
                .forEach(e -> {
                    Money debtValue = e.getSapRequestSet().stream().filter(sr -> sr.getRequestType() == SapRequestType.DEBT).map(sr -> sr.getValue())
                            .reduce(Money.ZERO, Money::add);
                    SapEvent sapEvent = new SapEvent(e);
                    final CreditEntry creditEntry = new CreditEntry("", new DateTime(), new LocalDate(), "", debtValue.getAmount()) {
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

                    Method method = null;
                    try {
                        method = SapEvent.class.getDeclaredMethod("registerDebtCredit", new Class[]{CreditEntry.class, Event.class, boolean.class});
                        method.setAccessible(true);
                        method.invoke(sapEvent, new Object[]{creditEntry, e, true});
                    } catch (NoSuchMethodException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    }
//                    registerDebtCredit(creditEntry, e, true);
//                    sapEvent.registerDebtCredit(creditEntry,event,true);
                });
    }
}