package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;

public class FixWronglyCreatedRegistrationDatasMobility extends CustomTask {
    @Override
    public void runTask() throws Exception {
        final RegistrationProtocol mobility = FenixFramework.getDomainObject("5295694675972");
        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final Spreadsheet spreadsheet = new Spreadsheet("RegistrationData");
        Bennu.getInstance().getRegistrationDataByExecutionYearSet().stream()
                .filter(rd -> rd.getRegistration().getRegistrationProtocol() == mobility)
                .filter(rd -> rd.getExecutionYear() == currentYear)
                .filter(rd -> rd.getRegistration().getStartExecutionYear() == currentYear.getNextExecutionYear())
                .filter(rd -> rd.getRegistration().getRegistrationDataByExecutionYearSet().stream()
                        .anyMatch(rd2 -> rd2.getExecutionYear() == currentYear.getNextExecutionYear()))
                .filter(rd -> rd.getRegistration().getEnrolments(currentYear).isEmpty())
                .forEach(rd -> {
                    report(rd, spreadsheet);
                    //rd.delete();
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("registration_data_deleted.xls", baos.toByteArray());
    }

    private void report(final RegistrationDataByExecutionYear rd, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Aluno", rd.getRegistration().getPerson().getUsername());
        row.setCell("Nome", rd.getRegistration().getName());
        row.setCell("Matrícula", rd.getRegistration().getDegreeName());
        row.setCell("Data Início", rd.getRegistration().getStartDate().toString());
        row.setCell("Registration Data", rd.getExecutionYear().getName());
        row.setCell("Registration Data OID", rd.getExternalId());
    }
}