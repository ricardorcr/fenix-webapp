package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class SeparateTransitionedSCPsAndApplications extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .filter(target -> target.getOutcomeConfigJson() != null)
                .filter(target -> target.getOutcomeConfigJson().has("degree"))
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> app.getDataObject().has("registration"))
                .forEach(this::fix);

    }

    private void fix(final Application application) {
        final String registrationID = application.getDataObject().get("registration").getAsString();
        final Registration registration = FenixFramework.getDomainObject(registrationID);
        if (registration.getStudentCurricularPlansSet().size() > 1) {
            final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
            if (scp.getStartExecutionYear() == ExecutionYear.readCurrentExecutionYear()) {
                taskLog("Separating %s %s %n", registration.getNumber(), scp.getDegreeCurricularPlan().getName());
            } else {
                taskLog("Something is wrong for: %s%n", registration.getNumber());
            }
        }
    }

    //final DegreeCurricularTransitionPlan transitionPlan = scp.getDegreeCurricularPlan().getOriginTransitionPlanSet().iterator().next();
}
