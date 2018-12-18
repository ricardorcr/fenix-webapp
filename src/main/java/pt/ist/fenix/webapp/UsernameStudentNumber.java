package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.ConnectSystem;

import java.io.ByteArrayOutputStream;

public class UsernameStudentNumber extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet usernames = new Spreadsheet("Usernames");
        final Spreadsheet userNumbers = usernames.addSpreadsheet("User-Number");
        ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(id -> id.getUser() != null)
                .forEach(identity -> {
                    final Spreadsheet.Row row = usernames.addRow();
                    row.setCell("User Principal", identity.getUser().getUsername());
                    identity.getAccountSet().stream()
                            .filter(account -> account.getUser() != null)
                            .filter(account -> account.getUser() != identity.getUser())
                            .forEach(account -> row.setCell("Outro User", account.getUsername()));
                    final Student student = identity.getUser().getPerson().getStudent();
                    if (student != null) {
                        student.getRegistrationsSet().stream()
                                .map(registration -> registration.getNumber())
                                .distinct()
                                .forEach(number -> {
                                    final Spreadsheet.Row row2 = userNumbers.addRow();
                                    row2.setCell("Username", identity.getUser().getUsername());
                                    row2.setCell("Nº Aluno", number);
                                });
                    }
                });

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        usernames.exportToXLSSheet(baos);
        output("mapa_username_nr_aluno.xlsx", baos.toByteArray());
    }
}
