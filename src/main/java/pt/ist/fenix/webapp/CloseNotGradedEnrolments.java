package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.curriculum.EnrollmentState;
import org.fenixedu.academic.domain.degreeStructure.CycleCourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CloseNotGradedEnrolments extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("HackedGrades");
        ExecutionYear.readCurrentExecutionYear().getPreviousExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(es -> es.getEnrolmentsSet().stream())
                .filter(enrolment -> enrolment.isEnroled())
                .filter(enrolment -> (!enrolment.getCurricularCourse().isDissertation()) || force(enrolment.getStudent()))
                .filter(enrolment -> isFirstOrSecondCycle(enrolment))
                .filter(enrolment -> enrolment.getEvaluationsSet().stream().noneMatch(ee -> ee.getEvaluationSeason().isExtraordinary()))
                .forEach(enrolment -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Semester", enrolment.getExecutionPeriod().getQualifiedName());
                    row.setCell("User", enrolment.getStudent().getPerson().getUsername());
                    row.setCell("Student Degree", enrolment.getRegistration().getDegree().getSigla());
                    row.setCell("CurricularCourse", enrolment.getCurricularCourse().getName());
                    row.setCell("CurricularCourse Degree", enrolment.getCurricularCourse().getDegreeCurricularPlan().getName());
                    row.setCell("Season", enrolment.getEvaluationSeason().getName().getContent());

                    //enrolment.setEnrollmentState(EnrollmentState.NOT_EVALUATED);
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("HackedGrades.xlsx", stream.toByteArray());
    }

    private boolean force(final Student student) {
        final int n = student.getNumber().intValue();
        final String username = student.getPerson().getUsername();
        return n == 68836
                || n == 78006
                || n == 65881
                || n == 81507
                || n == 81106
                || n == 86254
                || n == 73663
                || n == 62695
                ;
    }

    private boolean isFirstOrSecondCycle(final Enrolment enrolment) {
        final Registration registration = enrolment.getRegistration();
        return registration.getDegreeType().getCycleTypes().stream()
                .anyMatch(cycleType -> cycleType == CycleType.FIRST_CYCLE || cycleType == CycleType.SECOND_CYCLE);
    }

}