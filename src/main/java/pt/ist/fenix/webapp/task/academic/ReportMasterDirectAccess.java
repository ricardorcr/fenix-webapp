package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class ReportMasterDirectAccess extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Alunos");
        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(Registration::isMasterDegreeOrBolonhaMasterDegree)
                .filter(r -> r.getStartExecutionYear().isCurrent())
                .filter(this::doesNotHaveApplication)
                .forEach(r -> report(r, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("ingresso_directo_mestrado.xlsx", baos.toByteArray());
    }

    private void report(final Registration registration, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Aluno", registration.getPerson().getUsername());
        row.setCell("Curso", registration.getDegree().getSigla());
    }

    private boolean doesNotHaveApplication(final Registration registration) {
        return registration.getPerson().getUser().getIdentity().getAccountSet().stream()
                .flatMap(acc -> acc.getApplicationSet().stream())
                .anyMatch(app -> {
                    if (app.getDataObject().has("registration")) {
                        final String registrationID = app.getDataObject().get("registration").getAsString();
                        return !registrationID.equals(registration.getExternalId());
                    }
                    return true;
                });
    }
}
