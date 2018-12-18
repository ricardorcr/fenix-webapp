package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ChangeISTPresident extends CustomTask {

    @Override
    public void runTask() throws Exception {
        UniversityUnit ul = FenixFramework.getDomainObject("107375257775");

        ul.setPresident(User.findByUsername("ist13267"));
    }
}
