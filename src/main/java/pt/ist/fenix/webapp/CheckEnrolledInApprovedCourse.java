package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CheckEnrolledInApprovedCourse extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Problems");
        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(semester -> semester.getEnrolmentsSet().stream())
                .filter(enrolment -> enrolment.getStudent().getNumber().intValue() == 87509)
                .filter(enrolment -> enrolment.getName().getContent().equals("Processamento de Minérios e Resíduos Sólidos"))
                .peek(enrolment -> taskLog("%s%n", enrolment.getCurricularCourse().getName()))
                .filter(enrolment -> enrolment.isEnroled())
                .peek(enrolment -> taskLog("Enrolled"))
                .filter(enrolment -> isAlreadyApproved(enrolment))
                .peek(enrolment -> taskLog("Already approved"))
                .forEach(enrolment -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("User", enrolment.getStudent().getPerson().getUsername());
                    row.setCell("Curricular Course", enrolment.getCurricularCourse().getName(enrolment.getExecutionPeriod()));
                    row.setCell("When Enrolled", enrolment.getCreationDateDateTime().toString("yyyy-MM-dd"));
                    row.setCell("Enrolled By", enrolment.getCreatedBy());
                });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("problems.xlsx", stream.toByteArray());
    }

    private boolean isAlreadyApproved(final Enrolment enrolment) {
        final CurricularCourse curricularCourse = enrolment.getCurricularCourse();
        final Student student = enrolment.getRegistration().getStudent();
        return student.getRegistrationsSet().stream()
                .flatMap(registration -> registration.getStudentCurricularPlanStream())
                .anyMatch(scp -> scp.isApproved(curricularCourse));
    }

}