package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CloseNotGradedEnrolments extends WriteCustomTask {

    ExecutionYear current = null;

    @Override
    public void runTask() throws Exception {
        current = ExecutionYear.readCurrentExecutionYear(); // 2024/2025
        final ExecutionYear exclude = ExecutionYear.readExecutionYearByName("2017/2018");

        final Spreadsheet spreadsheet = new Spreadsheet("HackedGrades");
        Bennu.getInstance().getRegistrationsSet().stream()
                //.filter(registration -> registration.isActive())
                .filter(registration -> !registration.getDegree().isEmpty())
                .filter(this::isFirstOrSecondCycle)
                .flatMap(Registration::getStudentCurricularPlanStream)
                .flatMap(StudentCurricularPlan::getEnrolmentStream)
                .filter(Enrolment::isEnroled)
                .filter(enrolment -> enrolment.getExecutionYear() != current && enrolment.getExecutionYear() != exclude)
                .filter(enrolment -> enrolment.getEvaluationsSet().stream().noneMatch(ee -> ee.getEvaluationSeason().isExtraordinary()))
                .filter(this::shouldClose)
                .forEach(enrolment -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Semester", enrolment.getExecutionPeriod().getQualifiedName());
                    row.setCell("User", enrolment.getStudent().getPerson().getUsername());
                    row.setCell("Student Degree", enrolment.getRegistration().getDegree().getSigla());
                    row.setCell("CurricularCourse", enrolment.getCurricularCourse().getName());
                    row.setCell("CurricularCourse Degree", enrolment.getCurricularCourse().getDegreeCurricularPlan().getName());
                    row.setCell("Season", enrolment.getEvaluationSeason().getName().getContent());

                    enrolment.setEnrollmentState(EnrollmentState.NOT_EVALUATED);
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("HackedGrades.xlsx", stream.toByteArray());
    }

    private boolean shouldClose(final Enrolment enrolment) {
        return //enrolment.getExecutionYear() != previous ||
                !enrolment.isDissertation()
               // || (enrolment.isDissertation() && enrolment.getExecutionYear() == previous && enrolment.getExecutionPeriod().getSemester().intValue() == 1)
               // || (enrolment.isDissertation() && shouldClose(enrolment.getRegistration().getNumber().intValue()))
            ;
    }

    private boolean isFirstOrSecondCycle(final Registration registration) {
        return registration.getDegreeType().getCycleTypes().stream()
                .anyMatch(cycleType -> cycleType == CycleType.FIRST_CYCLE || cycleType == CycleType.SECOND_CYCLE);
    }
}