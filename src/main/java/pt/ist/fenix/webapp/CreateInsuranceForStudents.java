package pt.ist.fenix.webapp;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class CreateInsuranceForStudents extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
//        List<String> lines = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/alunos_mobilidade_criar_seguro_2020.txt").toPath());
        List<String> lines = Files.readAllLines(new File("/home/rcro/DocumentsHDD/fenix/alunos_mobilidade_criar_seguro_2020.txt").toPath());
        for (String username : lines) {
            User user = User.findByUsername(username);
            if (!user.getPerson().hasInsuranceEventOrAdministrativeOfficeFeeInsuranceEventFor(executionYear)) {
                new InsuranceEvent(user.getPerson(), executionYear);
            }
        }        
    }
}
