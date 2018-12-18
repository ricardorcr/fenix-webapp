package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.fenixedu.connect.domain.Identity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class UsersMail extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Emails");
        List<String> allLines = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/users_get_email.txt").toPath());
        for (String username : allLines) {
            User user = User.findByUsername(username);
            Row row = spreadsheet.addRow();
            row.setCell("Username", username);
            Identity identity = user.getIdentity();
            String email = null;
//            if (identity != null) {
//                Optional<ExternalCandidate> candidate = identity.getCandidateSet().stream()
//                    .filter(ExternalCandidate.class::isInstance)
//                    .map(c -> (ExternalCandidate)c)
//                    .findAny();
//                if (candidate.isPresent()) {
//                    email = candidate.get().getEmail();
//                }
//            } else if (email == null) {                
                EmailAddress emailAddress = user.getPerson().getDefaultEmailAddress();
                email = emailAddress != null ? emailAddress.getValue() : "-";
//            }
            row.setCell("Email", email);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Users_emails.xls", baos.toByteArray());
    }
}