package pt.ist.fenix.webapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.FenixFramework;

public class PersistDebt extends CustomTask {

	@Override
	public void runTask() throws Exception {
		Bennu.getInstance().getAccountingEventsSet().stream().filter(AdministrativeOfficeFeeEvent.class::isInstance)
				.forEach(event -> {
					FenixFramework.atomic(() -> {
						Method method = null;
						try {
							method = Event.class.getDeclaredMethod("persistDueDateAmountMap");
							method.setAccessible(true);
							method.invoke(event);
						} catch (NoSuchMethodException e1) {
							e1.printStackTrace();
						} catch (IllegalAccessException e1) {
							e1.printStackTrace();
						} catch (InvocationTargetException e1) {
							e1.printStackTrace();
						}
						;
					});
				});
	}
}