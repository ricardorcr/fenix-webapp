package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportNAsPaymentsDocuments extends CustomTask {

    @Override
    public void runTask() throws Exception {
        List<String> documentNumbers = null;
        try {
            documentNumbers = Files.readAllLines(
                    new File("/home/rcro/DocumentsHDD/fenix/sap/NAs_advancements.txt").toPath());
        } catch (Exception e) {
            throw new Error(e);
        }
        documentNumbers.forEach(naNumber -> {
            final Set<SapRequest> sapRequests = SapRoot.getInstance().getSapRequestSet().stream()
                    .filter(sr -> sr.getRequestType().equals(SapRequestType.ADVANCEMENT))
                    .filter(sr -> sr.getRequest().contains(naNumber))
                    .collect(Collectors.toSet());
            if (sapRequests.size() == 0 || sapRequests.size() > 1) {
                taskLog("Houston we have a problem: %s%n", naNumber);
            } else {
                final SapRequest sapRequest = sapRequests.iterator().next();
                taskLog("%s\t%s%n", sapRequest.getDocumentNumber(), sapRequest.getSapDocumentNumber());
            }
        });
    }
}
