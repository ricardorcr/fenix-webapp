package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

import pt.ist.fenixframework.FenixFramework;

public class ResetNumberOfMobileValidationAttempts extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getPartysSet().stream()
            .filter(Person.class::isInstance)
            .map(p -> (Person)p)
            .forEach(p -> {  
                FenixFramework.atomic(() -> {
                    p.setNumberOfValidationRequests(0);
                });
            });
    }
}
