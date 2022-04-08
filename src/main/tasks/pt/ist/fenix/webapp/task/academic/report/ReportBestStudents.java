package pt.ist.fenix.webapp.task.academic.report;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

public class ReportBestStudents extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("students");

        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(executionSemester -> executionSemester.getEnrolmentsSet().stream())
                .map(enrolment -> enrolment.getStudentCurricularPlan())
                .distinct()
                .filter(studentCurricularPlan -> considerDegree(studentCurricularPlan.getDegree()))
                .map(studentCurricularPlan -> studentCurricularPlan.getRegistration().getStudent())
                .distinct()
                .forEach(student -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("TecnicoID", student.getPerson().getUsername());
                    row.setCell("Name", student.getPerson().getUser().getProfile().getDisplayName());
                    row.setCell("E-mail", student.getPerson().getUser().getEmail());
                    row.setCell("Gender", student.getPerson().getGender() == null ? "" : student.getPerson().getGender().getLocalizedName())
                    row.setCell("Mobile", mobile(student.getPerson()));
                    row.setCell("Current Degree", student.getRegistrationsSet().stream()
                            .filter(registration -> registration.hasAnyEnrolmentsIn(ExecutionYear.readCurrentExecutionYear()))
                            .filter(registration -> !registration.getDegree().isEmpty())
                            .map(registration -> registration.getDegree().getSigla())
                            .collect(Collectors.joining(", ")));
                    row.setCell("ECTS", student.getRegistrationsSet().stream()
                            .filter(registration -> considerDegree(registration.getDegree()))
                            .mapToDouble(registration -> registration.getEctsCredits())
                            .sum());
                    final double ects1 = student.getRegistrationsSet().stream()
                            .filter(registration -> considerDegree(registration.getDegree()))
                            .filter(registration -> registration.getDegreeType().isFirstCycle())
                            .mapToDouble(registration -> registration.getEctsCredits())
                            .sum();
                    row.setCell("ECTS 1st Cycle", ects1);
                    final double ects2 = student.getRegistrationsSet().stream()
                            .filter(registration -> considerDegree(registration.getDegree()))
                            .filter(registration -> registration.getDegreeType().isSecondCycle())
                            .mapToDouble(registration -> registration.getEctsCredits())
                            .sum();
                    row.setCell("ECTS 2nd Cycle", ects2);
                    row.setCell("Average 1st Cycle", ects1 == 0d ? BigDecimal.ZERO : student.getRegistrationsSet().stream()
                            .filter(registration -> considerDegree(registration.getDegree()))
                            .filter(registration -> registration.getDegreeType().isFirstCycle())
                            .map(registration -> registration.calculateRawGrade().getNumericValue().multiply(new BigDecimal(registration.getEctsCredits())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(new BigDecimal(ects1), 2, RoundingMode.HALF_EVEN));
                    row.setCell("Average 2nd Cycle", ects2 == 0d ? BigDecimal.ZERO : student.getRegistrationsSet().stream()
                            .filter(registration -> considerDegree(registration.getDegree()))
                            .filter(registration -> registration.getDegreeType().isSecondCycle())
                            .map(registration -> registration.calculateRawGrade().getNumericValue().multiply(new BigDecimal(registration.getEctsCredits())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(new BigDecimal(ects2), 2, RoundingMode.HALF_EVEN));
                    addGradeInfo(row, student, "Fundamentos da Programação");
                    addGradeInfo(row, student, "Lógica para Programação");
                    addGradeInfo(row, student, "Programação com Objectos");
                    addGradeInfo(row, student, "Análise e Síntese de Algoritmos");
                    addGradeInfo(row, student, "Bases de Dados");
                    addGradeInfo(row, student, "Inteligência Artificial");
                    addGradeInfo(row, student, "Redes de Computadores");
                    addGradeInfo(row, student, "Engenharia de Software");
                    addGradeInfo(row, student, "Sistemas Distribuídos");
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("students.xls", stream.toByteArray());
    }

    private void addGradeInfo(final Spreadsheet.Row row, final Student student, final String courseName) {
        row.setCell(courseName, student.getRegistrationsSet().stream()
                .flatMap(registration -> registration.getApprovedEnrolments().stream())
                .filter(enrolment -> enrolment.getCurricularCourse().getName().indexOf(courseName) >= 0)
                .map(enrolment -> enrolment.getGradeValue())
                .collect(Collectors.joining(", ")));
    }

    private String mobile(final Person person) {
        return person.getPartyContactsSet().stream()
                .filter(partyContact -> partyContact.isMobile())
                .filter(partyContact -> partyContact.isActiveAndValid())
                .map(MobilePhone.class::cast)
                .map(mobilePhone -> mobilePhone.getPresentationValue())
                .findAny().orElse("");
    }

    private boolean considerDegree(final Degree degree) {
        final String code = degree.getSigla();
        return code != null && (code.indexOf("LEIC") >= 0
                || code.indexOf("MEIC") >= 0
                || code.indexOf("MEEC") >=0
                || code.indexOf("LETI") >= 0
                || code.indexOf("METI") >= 0
                || code.indexOf("LERC") >= 0
                || code.indexOf("MERC") >= 0);
    }

}
