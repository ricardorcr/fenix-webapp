package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CheckStudentsWithoutPlans extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("NoPlans");
        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(registration -> registration.isActive())
                .forEach(registration -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("user", registration.getPerson().getUsername());
                    row.setCell("degree", registration.getDegree().getSigla());
                    row.setCell("lastEnrolledYear", registration.getRegistrationDataByExecutionYearSet().stream()
                            .max((d1, d2) -> d1.compareTo(d2))
                            .map(d -> d.getExecutionYear().getYear())
                            .orElse(""));
                });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("noplans.xlsx", stream.toByteArray());
    }

}