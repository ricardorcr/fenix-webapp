package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;

import java.util.HashMap;
import java.util.Map;

public class UpdateManualFixedSAPCreditDebts extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("NJ1288488","3220077669/2024");
        map.put("NJ1288340","3220077670/2024");
        map.put("NJ1288335","3220077671/2024");
        map.put("NJ1288231","3220077672/2024");
        map.put("NJ1257599","3220077673/2024");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            final SapRequest sapRequest = getSapRequest(entry.getKey());
            sapRequest.setSapDocumentNumber(entry.getValue());
            sapRequest.setIntegrated(true);
        }
    }

    private SapRequest getSapRequest(final String documentNumber) {
        return SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(documentNumber))
                .findAny().get();
    }
}
