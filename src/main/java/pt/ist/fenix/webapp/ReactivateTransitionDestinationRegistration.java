package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

public class ReactivateTransitionDestinationRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Degree.readBolonhaDegrees().stream()
                .flatMap(degree -> degree.getRegistrationsSet().stream())
                .filter(r -> r.getActiveState().getStateType().equals(RegistrationStateType.TRANSITED))
                .filter(this::isDestination)
                .forEach(r -> {
                    //r.getActiveState().delete();
                    taskLog("Deleted transited state for: %s\t%s%n", r.getPerson().getUsername(), r.getDegree().getSigla());
                });
    }

    private boolean isDestination(Registration registration) {
        return registration.getStudent().getStudentDegreeCurricularTransitionPlanSet().stream()
                .anyMatch(stp ->
                        isSameDCP(stp.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan(), registration));
    }



    private boolean isSameDCP(DegreeCurricularPlan destinationDegreeCurricularPlan, Registration registration) {
        return registration.getStudentCurricularPlansSet().stream()
                .anyMatch(scp -> scp.getDegreeCurricularPlan() == destinationDegreeCurricularPlan);
    }
}
