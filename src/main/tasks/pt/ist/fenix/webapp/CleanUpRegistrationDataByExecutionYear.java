package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class CleanUpRegistrationDataByExecutionYear extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final RegistrationDataByExecutionYear dataByExecutionYear = FenixFramework.getDomainObject("847293968396902");
        dataByExecutionYear.delete();
    }
}