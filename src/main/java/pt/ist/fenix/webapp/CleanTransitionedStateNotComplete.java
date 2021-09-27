package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

public class CleanTransitionedStateNotComplete extends CustomTask {

    @Override
    public void runTask() throws Exception {
        DateTime transitedDate = new DateTime(2021, 9, 13, 0, 0);
        Degree.readBolonhaDegrees().stream()
                .flatMap(degree -> degree.getRegistrationsSet().stream())
                .filter(r -> r.getActiveState().getStateType().equals(RegistrationStateType.TRANSITED))
                .filter(r -> {
                    //ensuring it was created by the transition script
                    final RegistrationState activeState = r.getActiveState();
                    return activeState.getStateDate().isAfter(transitedDate) && activeState.getResponsiblePerson() == r.getPerson();
                } )
                .filter(this::isToDeleteState)
                .forEach(r -> {
                    //r.getActiveState().delete();
                    taskLog("Deleted transited state for: %s\t%s%n", r.getPerson().getUsername(), r.getDegree().getSigla());
                });
    }

    private boolean isToDeleteState(Registration registration) {
        if (registration.getStudent().getRegistrationsSet().size() == 1) {
            return true;
        } else {
            return !registration.getStudent().getRegistrationStream()
                    .filter(r -> r != registration)
                    .anyMatch(r -> r.getStudent().getStudentDegreeCurricularTransitionPlanSet().stream()
                                    .anyMatch(stp ->
                                            isSameDCP(stp.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan(), r)));
        }
    }

    private boolean isSameDCP(DegreeCurricularPlan destinationDegreeCurricularPlan, Registration registration) {
        return registration.getStudentCurricularPlansSet().stream()
                .anyMatch(scp -> scp.getDegreeCurricularPlan() == destinationDegreeCurricularPlan);
    }
}
