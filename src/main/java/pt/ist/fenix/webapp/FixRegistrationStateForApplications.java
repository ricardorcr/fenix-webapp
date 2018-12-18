package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class FixRegistrationStateForApplications extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> "degree".equals(ap.getOutcomeTypeJson().get("name").getAsString()))
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> app.getDataObject().has("registration"))
                .map(this::getRegistration)
                .filter(r -> r.getActiveState().getStateType() != RegistrationStateType.REGISTERED &&
                        r.getActiveState().getStateType() != RegistrationStateType.CANCELED)
                .forEach(this::fix);
    }

    private void fix(Registration registration) {
        taskLog("Created registered state for: %s\t%s\t%s\t%s%n", registration.getPerson().getUsername(), registration.getDegreeCurricularPlanName(),
                registration.getExternalId(), registration.getActiveState().getStateType());
        RegistrationState.createRegistrationStateWithoutValidation(registration, registration.getPerson(), new DateTime(),
                RegistrationStateType.REGISTERED, null);
    }

    private Registration getRegistration(Application application) {
        final String registrationOID = application.getDataObject().get("registration").getAsString();
        return FenixFramework.getDomainObject(registrationOID);
    }
}
