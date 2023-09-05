package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class ReportAllowedOpen2ndCycle extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear startYear = ExecutionYear.readExecutionYearByName("2019/2020");
        final ExecutionYear endYear = ExecutionYear.readCurrentExecutionYear();
        final Spreadsheet spreadsheet = new Spreadsheet("Matriculas");

        startYear.getRegistrationDataByExecutionYearSet().stream()
//                .filter(rdey -> rdey.getEnrolmentDate() != null)
                .map(RegistrationDataByExecutionYear::getRegistration)
                .filter(registration -> registration.getDegree().isFirstCycle())
//                .filter(registration -> !registration.getDegree().isSecondCycle())
//                .filter(registration -> registration.isActive())
                .filter(registration -> registration.getLastStudentCurricularPlan().getSecondCycle() == null)
                .filter(registration -> registration.getLastStudentCurricularPlan().getFirstCycle() != null)
                .filter(registration -> !registration.getLastStudentCurricularPlan().getFirstCycle().isConclusionProcessed())
                .forEach(registration -> report(spreadsheet, registration, startYear, endYear));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("alunos_matriculados.xlsx", baos.toByteArray());
    }

    private void report(final Spreadsheet spreadsheet, final Registration registration, final ExecutionYear startYear, final ExecutionYear endYear) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("IstID", registration.getPerson().getUsername());
        row.setCell("Curso", registration.getDegreeCurricularPlanName());
        row.setCell(startYear.getName(), "Sim");
        ExecutionYear nextYear = startYear.getNextExecutionYear();
        while (nextYear.isBeforeOrEquals(endYear)) {
            row.setCell(nextYear.getName(), hasActiveRegistrationDataFor(nextYear, registration) ? "Sim" : "Não");
            nextYear = nextYear.getNextExecutionYear();
        }
    }

    private boolean hasActiveRegistrationDataFor(final ExecutionYear executionYear, final Registration registration) {
        final boolean result = registration.getRegistrationDataByExecutionYearSet().stream()
                .anyMatch(rd -> rd.getExecutionYear() == executionYear);
        boolean activeContinuity = false;
        if (result) {
            final boolean anyMatch = registration.getStudentCurricularPlansSet().stream()
                    .flatMap(scp -> scp.getAllCurriculumLines().stream())
                    .filter(CurriculumLine::hasExecutionPeriod)
                    .anyMatch(cl -> cl.getExecutionYear() == executionYear);
            if (!anyMatch) {
                taskLog("check this case: %s\t%s\t%s%n", registration.getPerson().getUsername(), executionYear.getName(), registration.getDegreeCurricularPlanName());
            }
        } else {
            activeContinuity = registration.getStudent().getRegistrationsSet().stream()
                    .filter(r -> registration == r.getSourceRegistration())
                    .flatMap(r -> r.getRegistrationDataByExecutionYearSet().stream())
                    .anyMatch(rd -> rd.getExecutionYear() == executionYear);
        }
        return result || activeContinuity;
    }
}
