package pt.ist.fenix.webapp;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.FenixFramework;

public class CheckSapErrors extends CustomTask {

    @Override
    public void runTask() throws Exception {
        List<String> allLines = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/erros_sap_nao_criado.csv").toPath());
        for (String line : allLines) {
            String[] lineSplit = line.split("\t");
            Event event = FenixFramework.getDomainObject(lineSplit[0]);
            event.getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(lineSplit[1]))
                .filter(this::isAdvancementUse)
                .forEach(this::print);
        }        
    }
    
    private boolean isAdvancementUse(final SapRequest sapRequest) {
        return sapRequest.getRequestType() == SapRequestType.PAYMENT &&
                sapRequest.getRequest().contains("isAdvancedPayment");
    }
    
    private void print(final SapRequest sr) {
        if (sr.getAdvancementRequest() != null) {
            taskLog("%s\t%s\t%s\t%s%n", sr.getDocumentNumber(), sr.getDocumentDate(), sr.getAdvancementRequest().getDocumentNumber(), sr.getAdvancementRequest().getDocumentDate());
        } else {
            taskLog("%s\t%s - sem adv request%n", sr.getDocumentNumber(), sr.getDocumentDate());
        }
    }
}
