package pt.ist.fenix.webapp;

import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckNPs extends CustomTask {

    @Override
    public void runTask() throws Exception {


        Set<String> allNPs = new HashSet<>();
        GroupBasedFile file = FenixFramework.getDomainObject("563568428782532");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(file.getContent())));
        String np = reader.readLine();
        StringBuilder sb = new StringBuilder();
        try {
            while (np != null) {
                if (!np.endsWith("_")) {
                    if (!allNPs.add(np)) {
                        /*SapRequest sr = findSapRequest(np);
                        if (sr.getAnulledRequest() != null || sr.getOriginalRequest() != null) {
                            //se é estorno remover porque se anulam
                            allNPs.remove(np);
                        } else {
                            taskLog("Este %s %s não é um estorno...%n", sr.getDocumentNumber(), sr.getEvent().getExternalId());
                        }*/
                    }
                }
                np = reader.readLine();
            }
        } finally {
            reader.close();
        }
        allNPs.stream().forEach(npLine -> sb.append(npLine).append("\n"));
        output("uniqueNPs2.txt", sb.toString().getBytes());
//
//        Set<SapRequest> notInSAPList = new HashSet<>();
//        SapRoot.getInstance().getSapRequestSet().stream()
//                .filter(sr -> !sr.isInitialization())
//                .filter(sr -> sr.getIntegrated())
//                //.filter(sr -> sr.getOriginalRequest() == null && sr.getAnulledRequest() == null)
//                .filter(sr -> sr.getSapDocumentNumber() != null)
//                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT ||
//                        sr.getRequestType() == SapRequestType.ADVANCEMENT ||
//                        sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
//                .filter(sr -> !allNPs.contains(sr.getDocumentNumber())).forEach(
//                sr -> {
//                    taskLog("%s - SapClientId: %s - FenixEvent: %s%n", sr.getDocumentNumber(), sr.getClientId(), sr.getEvent().getExternalId());
//                    notInSAPList.add(sr);
//                });
//
//        taskLog("Total pagamentos em Fénix que não constam na lista SAP: %s%n",
//                notInSAPList.stream().filter(sr -> sr.getOriginalRequest() == null && sr.getAnulledRequest() == null)
//                        .map(sr -> sr.getValue().add(sr.getAdvancement())).reduce(Money.ZERO, Money::add).toString());


        //TODO important!!
        //1 - os NP's que o fénix não tem são derivados das notas de crédito -> fazer query para confirmar que todos os casos são disso
        //2 - somar o valor dessas NA pq estão a ser contabilizadas no ficheiro e ver se a diferença bate certo com esta soma!
        Set<SapRequest> requests = SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getSapDocumentNumber() != null)
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT ||
                        sr.getRequestType() == SapRequestType.PAYMENT_INTEREST ||
                        sr.getRequestType() == SapRequestType.ADVANCEMENT).collect(Collectors.toSet());

        allNPs.stream().filter(npNumber -> findSapRequest(requests, npNumber))
                .forEach(npNumber -> taskLog("O Fénix não tem %s%n", npNumber));
    }

    private SapRequest findSapRequest(final String np) {
        return SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equalsIgnoreCase(np)).findAny().get();
    }

    private boolean findSapRequest(final Set<SapRequest> requests, final String npNumber) {
        return !requests.stream().filter(sr -> sr.getDocumentNumber().equalsIgnoreCase(npNumber)).findAny().isPresent();
    }
}