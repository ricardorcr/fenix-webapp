package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CheckEnrolmentsInDegreeWithNoExecution extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Inscrições no curso errado");
        ExecutionSemester.readActualExecutionSemester().getEnrolmentsSet().stream()
                .filter(enrolment -> !enrolment.getDegreeCurricularPlanOfDegreeModule().getExecutionDegreesSet().stream()
                        .anyMatch(ed -> ed.getExecutionYear().isCurrent()))
                .forEach(enrolment -> report(enrolment, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("inscricoes_curso_errado.xls", baos.toByteArray());
    }

    private void report(final Enrolment enrolment, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        final Person student = enrolment.getRegistration().getPerson();
        row.setCell("IstID", student.getUsername());
        row.setCell("Nome", student.getName());
        row.setCell("Disciplina", enrolment.getName().getContent());
        row.setCell("Curso Disciplina", enrolment.getDegreeCurricularPlanOfDegreeModule().getName());
        row.setCell("Curso Aluno", enrolment.getStudentCurricularPlan().getDegreeCurricularPlan().getName());
        row.setCell("Protocolo", enrolment.getRegistration().getRegistrationProtocol().getDescription().getContent());
    }
}
