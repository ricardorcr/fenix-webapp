package pt.ist.fenix.webapp.task.academic.report;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class StudentPerformanceReport extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("StudentPerformance");
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        executionYear.getRegistrationDataByExecutionYearSet().stream()
                .filter(data -> data.getRegistration().getDegree().isFirstCycle() || data.getRegistration().getDegree().isSecondCycle())
                .forEach(data -> {
            final Registration registration = data.getRegistration();
            final Student student = registration.getStudent();
            final Person person = student.getPerson();
            final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
            final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();
            final Degree degree = degreeCurricularPlan.getDegree();
            final CycleType cycleType = registration.getCycleType(executionYear);
            final Double maxCredits = data.getMaxCreditsPerYear();
            final ExecutionSemester allowedSemester = data.getAllowedSemesterForEnrolments();
            final ExecutionYear ingressionYear = executionYears(registration).min(ExecutionYear::compareTo).orElse(null);

            final Supplier<Stream<Enrolment>> enrolments = () -> studentCurricularPlan.getEnrolmentStream()
                    .filter(enrolment -> enrolment.getExecutionYear() == executionYear);

            spreadsheet.addRow()
                    .setCell("User", person.getUsername())
                    .setCell("Name", person.getName())
                    .setCell("Degree Type", degree.getDegreeType().getName().getContent())
                    .setCell("Curricular Plan", degreeCurricularPlan.getName())
                    .setCell("Ingression Year", ingressionYear == null ? null : ingressionYear.getName())
                    .setCell("Protocol", registration.getRegistrationProtocol() == null ? ""
                            : registration.getRegistrationProtocol().getDescription().getContent())
                    .setCell("Ingression Type", registration.getIngressionType() == null ? ""
                            : registration.getIngressionType().getLocalizedName())
                    .setCell("Cycle", cycleType == null ? null : cycleType.getDescriptionI18N().getContent())
                    .setCell("Enrolment Date", data.getEnrolmentDate().toString("yyyy-MM-dd"))
                    .setCell("Max Credits Allowed", maxCredits == null ? "" : maxCredits.toString())
                    .setCell("Allowed Semester", allowedSemester == null ? "" : executionYear.getName())

                    .setCell("Enrolment Count", Long.toString(enrolments.get().count()))
                    .setCell("Enrolment ECTS", Double.toString(enrolments.get()
                            .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue())
                            .sum()))
                    .setCell("Approved Count", Long.toString(enrolments.get().filter(e -> e.isApproved()).count()))
                    .setCell("Approved ECTS", Double.toString(enrolments.get()
                            .filter(e -> e.isApproved())
                            .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue())
                            .sum()))

                    .setCell("Enrolment Count Sem 1", Long.toString(enrolments.get()
                            .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 1)
                            .count()))
                    .setCell("Enrolment ECTS Sem 1", Double.toString(enrolments.get()
                            .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 1)
                            .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue())
                            .sum()))
                    .setCell("Approved Count Sem 1", Long.toString(enrolments.get()
                            .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 1)
                            .filter(e -> e.isApproved())
                            .count()))
                    .setCell("Approved ECTS Sem 1", Double.toString(enrolments.get()
                            .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 1)
                            .filter(e -> e.isApproved())
                            .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue())
                            .sum()))

                    .setCell("Enrolment Count Sem 2", Long.toString(enrolments.get()
                            .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 2)
                            .count()))
                    .setCell("Enrolment ECTS", Double.toString(enrolments.get()
                            .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 2)
                            .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue())
                            .sum()))
                    .setCell("Approved Count Sem 2", Long.toString(enrolments.get()
                            .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 2)
                            .filter(e -> e.isApproved())
                            .count()))
                    .setCell("Approved ECTS", Double.toString(enrolments.get()
                            .filter(e -> e.getExecutionPeriod().getSemester().intValue() == 2)
                            .filter(e -> e.isApproved())
                            .mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue())
                            .sum()))
                    ;
        });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("StudentPerformance_" + executionYear.getName().replace("/", "_"), stream.toByteArray());
    }

    private Stream<ExecutionYear> executionYears(final Registration registration) {
        Stream<ExecutionYear> stream = registration.getRegistrationDataByExecutionYearSet().stream()
                .map(data -> data.getExecutionYear());
        stream = executionYears(stream, registration.getSourceRegistration());
        return executionYears(stream, registration.getSourceRegistrationForTransition());
    }

    private Stream<ExecutionYear> executionYears(final Stream<ExecutionYear> stream, final Registration registration) {
        return registration == null ? stream : Stream.concat(stream, executionYears(registration));
    }

}