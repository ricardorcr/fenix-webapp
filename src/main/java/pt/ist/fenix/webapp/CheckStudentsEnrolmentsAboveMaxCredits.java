package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CheckStudentsEnrolmentsAboveMaxCredits extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Alunos");
        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        Bennu.getInstance().getDegreeCurricularPlansSet().stream()
                .filter(dcp -> dcp.getExecutionDegreeByYear(currentYear) != null)
                .flatMap(dcp -> dcp.getStudentCurricularPlansSet().stream())
                .filter(scp -> scp.hasEnrolments(currentYear))
                .forEach(scp -> {
                    final Double firstSemester = scp.getEnrolmentsByExecutionPeriod(currentYear.getFirstExecutionPeriod()).stream()
                            .map(Enrolment::getEctsCredits)
                            .reduce(Double.valueOf(0), Double::sum);

                    final Double secondSemester = scp.getEnrolmentsByExecutionPeriod(currentYear.getLastExecutionPeriod()).stream()
                            .map(Enrolment::getEctsCredits)
                            .reduce(Double.valueOf(0), Double::sum);
                    if (firstSemester > 36.0 || secondSemester > 36.0) {
                        report(scp, spreadsheet, firstSemester, secondSemester);
                    }
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);

        output("Alunos_acima_maximo_creditos_22-23.xlsx", baos.toByteArray());
    }

    private void report(final StudentCurricularPlan scp, final Spreadsheet spreadsheet, final Double firstSemester, final Double secondSemester) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Aluno", scp.getRegistration().getNumber());
        row.setCell("Curso", scp.getName());
        row.setCell("Créditos 1sem", firstSemester);
        row.setCell("Créditos 2sem", secondSemester);
    }
}
