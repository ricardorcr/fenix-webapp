package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class DeleteRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Registration registration = FenixFramework.getDomainObject("1128159059642232");
        registration.getPrecedentDegreesInformationsSet().forEach(pdi -> {
            pdi.getPersonalIngressionData().delete();
            pdi.delete();
        });
        registration.delete();
    }
}
