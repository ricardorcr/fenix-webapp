package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class ReportAutomaticConclusionProcesses extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        //TODO confirmar que todas as condições que são validadas na interface (no jsp) estão aqui
        final Spreadsheet spreadsheet = new Spreadsheet("Alunos");
        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(registration -> registration.getConclusionProcess() == null)
                .filter(registration -> registration.getDegree().isFirstCycle())
                .filter(registration -> registration.getLastStudentCurricularPlan() != null)
                .filter(registration -> registration.getLastStudentCurricularPlan().getFirstCycle() != null)
                .filter(registration -> !registration.getLastStudentCurricularPlan().getFirstCycle().isConclusionProcessed())
                .filter(registration -> registration.getLastStudentCurricularPlan().getFirstCycle().isConcluded())
                .filter(registration -> {
                    final CycleCurriculumGroup firstCycle = registration.getLastStudentCurricularPlan().getFirstCycle();
                    return !firstCycle.getAllCurriculumLines().stream()
                            .anyMatch(cl -> cl.isCreditsDismissal() || cl.isDismissal());
                })
                .forEach(registration -> report(spreadsheet, registration));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("apuramentos_automaticos_possiveis.xlsx", baos.toByteArray());
    }

    private void report(final Spreadsheet spreadsheet, final Registration registration) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("IstID", registration.getPerson().getUsername());
        row.setCell("Nome", registration.getPerson().getName());
        row.setCell("Curso", registration.getDegreeCurricularPlanName());
    }
}
