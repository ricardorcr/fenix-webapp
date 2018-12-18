package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

public class UsersMobile extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Telemóveis");
        List<String> allLines = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/users_get_cell.txt").toPath());
        for (String username : allLines) {
            User user = User.findByUsername(username);
            Row row = spreadsheet.addRow();
            row.setCell("Username", username);
//            Identity identity = user.getPerson().getIdentity();
            String mobile = null;
//            if (identity != null) {
//                Optional<ExternalCandidate> candidate = identity.getCandidateSet().stream()
//                    .filter(ExternalCandidate.class::isInstance)
//                    .map(c -> (ExternalCandidate)c)
//                    .findAny();
//                if (candidate.isPresent()) {
//                    mobile = candidate.get().getMobile();
//                }
//            } else if (mobile == null) {                
                MobilePhone mobilePhone = user.getPerson().getDefaultMobilePhone();
                mobile = mobilePhone != null ? mobilePhone.getNumber() : "-";
//            }
            row.setCell("Telemóvel", mobile);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Users_telemoveis.xls", baos.toByteArray());
    }
}