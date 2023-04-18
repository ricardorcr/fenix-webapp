package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curricularRules.executors.RuleResult;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.service.services.student.enrolment.bolonha.EnrolBolonhaStudent;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixEnrolmentsInApprovedCourses extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        List<ExecutionYear> executionYearList = new ArrayList<>();
        ExecutionYear startExecutionYear = ExecutionYear.readExecutionYearByName("2015/2016");
        for (ExecutionYear executionYear = startExecutionYear; executionYear.getNextExecutionYear() != null; executionYear = executionYear.getNextExecutionYear()) {
            executionYearList.add(executionYear);
        }
        executionYearList.stream()
                .flatMap(ey -> ey.getExecutionPeriodsSet().stream())
                .flatMap(semester -> semester.getEnrolmentsSet().stream())
                .filter(enrolment -> enrolment.isActive())
                .filter(enrolment -> !enrolment.isImprovementEnroled())
                .filter(enrolment -> !enrolment.hasAssociatedMarkSheetOrFinalGrade() && !enrolment.isAnnulled() && enrolment.getEnrollmentState() != EnrollmentState.NOT_EVALUATED)
                .filter(enrolment -> enrolment.getRegistration().isCurricularCourseApproved(enrolment.getCurricularCourse()))
                .filter(this::isApprovedBefore)
                .forEach(this::unenroll);
    }

    private boolean isApprovedBefore(final Enrolment enrolment) {
        return enrolment.getRegistration().getApprovedEnrolments().stream()
                .filter(e -> e.getCurricularCourse() == enrolment.getCurricularCourse())
                .anyMatch(approved -> approved.getExecutionPeriod().isBefore(enrolment.getExecutionPeriod()));
    }

    private void unenroll(final Enrolment enrolment) {
        FenixFramework.atomic(() -> {
            final String username = enrolment.getRegistration().getPerson().getUsername();
            taskLog("%s\t%s\t%s\t%s%n", username, enrolment.getStudentCurricularPlan().getName(), enrolment.getExecutionPeriod().getQualifiedName(), enrolment.getName().getContent());
//            final RuleResult ruleResults =
//                    EnrolBolonhaStudent.run(enrolment.getStudentCurricularPlan(),
//                            enrolment.getExecutionPeriod(),
//                            Collections.emptyList(),
//                            Collections.singletonList(enrolment),
//                            CurricularRuleLevel.ENROLMENT_WITH_RULES);
//
//            if (!ruleResults.isTrue()) {
//                taskLog("Problems for: %s\t%s\t%s\t%s%n", enrolment.getExternalId(), enrolment.getName().getContent(),
//                        username, ruleResults.getMessages());
//            }
        });
    }
}
