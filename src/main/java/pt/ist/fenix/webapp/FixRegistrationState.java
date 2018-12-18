package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class FixRegistrationState extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Registration registrationNewScp = FenixFramework.getDomainObject("2259152942311");
        final Registration registrationOldScp = FenixFramework.getDomainObject("2259153201439");

        final DateTime whenDateTime = registrationNewScp.getLastStudentCurricularPlan().getWhenDateTime();
        RegistrationState.createRegistrationStateWithoutValidation(registrationNewScp, registrationNewScp.getPerson(), whenDateTime, RegistrationStateType.REGISTERED, null);
        RegistrationState.createRegistrationState(registrationOldScp, registrationOldScp.getPerson(), whenDateTime, RegistrationStateType.TRANSITED);
    }
}

