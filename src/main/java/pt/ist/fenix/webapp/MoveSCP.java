package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class MoveSCP extends CustomTask {

    @Override
    public void runTask() throws Exception {
        //old registration 2259153434915 leic-A 2021
        final Registration oldRegistration = FenixFramework.getDomainObject("2259153434915");
        //new registration 1691109013062885
        final Registration newRegistration = FenixFramework.getDomainObject("1691109013062885");

        final StudentCurricularPlan newSCP = oldRegistration.getStudentCurricularPlanStream()
                .filter(scp -> scp.getName().equals("LEIC-A 2021"))
                .findAny().get();

        newSCP.setRegistration(newRegistration);
    }
}
