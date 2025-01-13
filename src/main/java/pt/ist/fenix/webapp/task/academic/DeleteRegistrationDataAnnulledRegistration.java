package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

public class DeleteRegistrationDataAnnulledRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Matrículas");
        ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        currentExecutionYear.getRegistrationDataByExecutionYearSet().stream()
                .filter(rdey -> rdey.getRegistration().isCanceled())
                .filter(rdey -> !rdey.getRegistration().hasAnyCurriculumLines(currentExecutionYear))
                .forEach(rdey -> {
                    Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("IstID", rdey.getRegistration().getStudent().getPerson().getUsername());
                    row.setCell("Matrícula", rdey.getRegistration().getDegree().getPresentationName());
                    row.setCell("Ano", currentExecutionYear.getName());
                    rdey.delete();
                });
        output("data_by_execution_deleted.xlsx", spreadsheet.exportToXLSXSheet());
    }
}
