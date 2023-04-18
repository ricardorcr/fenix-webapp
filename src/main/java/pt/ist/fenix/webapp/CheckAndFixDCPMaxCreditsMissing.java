package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.curricularRules.MaximumNumberOfCreditsForEnrolmentPeriod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CheckAndFixDCPMaxCreditsMissing extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Malandros");
        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        Bennu.getInstance().getDegreeCurricularPlansSet().stream()
                .filter(dcp -> dcp.getExecutionDegreeByYear(currentYear) != null)
                .filter(dcp -> dcp.getRoot().getCurricularRulesSet().isEmpty())
                .peek(dcp -> {
                    dcp.getStudentCurricularPlansSet().stream()
                            .filter(scp -> scp.hasEnrolments(currentYear))
                            .filter(scp -> {
                                final Double firstSemester = scp.getEnrolmentsByExecutionPeriod(currentYear.getFirstExecutionPeriod()).stream()
                                        .map(Enrolment::getEctsCredits)
                                        .reduce(Double.valueOf(0), Double::sum);

                                final Double secondSemester = scp.getEnrolmentsByExecutionPeriod(currentYear.getLastExecutionPeriod()).stream()
                                        .map(Enrolment::getEctsCredits)
                                        .reduce(Double.valueOf(0), Double::sum);
                                return firstSemester > 36.0 || secondSemester > 36.0;
                            })
                            .forEach(scp -> report(scp, spreadsheet));
                })
                .forEach(dcp -> {
                    taskLog("%s%n", dcp.getName());
                    new MaximumNumberOfCreditsForEnrolmentPeriod(dcp.getRoot(), currentYear.getPreviousExecutionYear().getFirstExecutionPeriod());
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Alunos_acima_maximo_creditos_22-23.xlsx", baos.toByteArray());
    }

    private void report(final StudentCurricularPlan scp, final Spreadsheet spreadsheet) {
        final Double firstSemester = scp.getEnrolmentsByExecutionPeriod(ExecutionYear.readCurrentExecutionYear().getFirstExecutionPeriod()).stream()
                .map(Enrolment::getEctsCredits)
                .reduce(Double.valueOf(0), Double::sum);

        final Double secondSemester = scp.getEnrolmentsByExecutionPeriod(ExecutionYear.readCurrentExecutionYear().getLastExecutionPeriod()).stream()
                .map(Enrolment::getEctsCredits)
                .reduce(Double.valueOf(0), Double::sum);
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Aluno", scp.getRegistration().getNumber());
        row.setCell("Curso", scp.getName());
        row.setCell("Créditos 1sem", firstSemester);
        row.setCell("Créditos 2sem", secondSemester);
    }
}
