package pt.ist.fenix.webapp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

public class GetNAsForNPs extends CustomTask {

    @Override
    public void runTask() throws Exception {

        List<String> nps = null;
        try {
            nps = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/nps_to_get_nas.txt").toPath());
//            nps = Arrays.asList("NP422506","NP411608","NP421029");
        } catch (IOException e) {
            throw new Error("Erro a ler o ficheiro.");
        }
        
        for (final String npNumber : nps) {
            String naNumber = SapRoot.getInstance().getSapRequestSet().stream()
                    .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                    .filter(sr -> sr.getRequest().contains(npNumber))
                    .findAny().get().getDocumentNumber();
            taskLog("%s for %s%n", naNumber, npNumber);
        }
    }
}