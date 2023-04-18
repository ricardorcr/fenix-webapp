package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixEmptyEnrolmentDateRegistrationData extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getRegistrationDataByExecutionYearSet().stream()
                .filter(rd -> rd.getEnrolmentDate() == null)
                .filter(rd -> !rd.getRegistration().getEnrolments(rd.getExecutionYear()).isEmpty())
                .forEach(rd -> {
                    taskLog("%s\t%s%n", rd.getRegistration().getNumber(), rd.getRegistration().getDegreeCurricularPlanName());
                    final Enrolment firstEnrolment = rd.getRegistration().getEnrolments(rd.getExecutionYear()).stream()
                            .min(Enrolment.COMPARATOR_BY_CREATION_DATE).get();
                    rd.edit(firstEnrolment.getCreationDateDateTime().toLocalDate(), rd.getEventTemplate());
                });
    }
}
