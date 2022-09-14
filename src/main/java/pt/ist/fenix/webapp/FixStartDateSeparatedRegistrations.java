package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;

public class FixStartDateSeparatedRegistrations extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        currentExecutionYear.getStudentsSet().stream()
                .filter(r -> r.getSourceRegistration() != null)
                .filter(r -> r.getDegree().isSecondCycle())
                .filter(r -> r.getSourceRegistration().getDegree().isFirstCycle())
                .filter(r -> r.getSourceRegistration().getLastState().getStateType() == RegistrationStateType.CONCLUDED)
                .filter(r -> r.getLastState().getResponsiblePerson() == null)
                .filter(r -> r.getRegistrationProtocol() == r.getSourceRegistration().getRegistrationProtocol())
                .forEach(r -> {
                    final CycleCurriculumGroup sourceFirstCycle = r.getSourceRegistration().getLastStudentCurricularPlan().getFirstCycle();
                    final ExecutionSemester conclusionSemester = getConclusionSemester(sourceFirstCycle);
                    final YearMonthDay beginDateYearMonthDay = conclusionSemester.getNextExecutionPeriod().getBeginDateYearMonthDay();
                    if (conclusionSemester != null && !beginDateYearMonthDay.isEqual(r.getStartDate())) {
                        taskLog("%s\t%s\t%s\t%s\t%s->data matrícula%n", r.getNumber(), r.getExternalId(), r.getDegreeCurricularPlanName(),
                                beginDateYearMonthDay.toString("dd/MM/yyyy"), r.getStartDate().toString("dd/MM/yyyy"));
                        if (r.getNumber() == 89861) { //TODO tem substituição 1sem 22/23 era suposto?
                            return;
                        }
                        r.setStartDate(beginDateYearMonthDay);
                        r.getLastState().setStateDate(beginDateYearMonthDay);
                    }
                });
    }

    private ExecutionSemester getConclusionSemester(final CycleCurriculumGroup firstCycle) {
        return firstCycle.getApprovedCurriculumLines().stream()
                .map(cl -> cl.getExecutionPeriod())
                .max(ExecutionSemester.COMPARATOR_BY_BEGIN_DATE)
                .orElseGet(() -> null);
    }
}
