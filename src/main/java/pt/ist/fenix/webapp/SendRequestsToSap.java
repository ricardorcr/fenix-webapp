package pt.ist.fenix.webapp;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class SendRequestsToSap extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        List<String> documentNumbers = null;
//        try {
//			documentNumbers = Files.readAllLines(
//					new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/reenvio_documentos_lote2_15_11_2020_NAs.txt").toPath());
            documentNumbers = Arrays.asList("NP635239");
//		} catch (IOException e) {
//			throw new Error("Erro a ler o ficheiro.");
//		}
        for (String documentNumber : documentNumbers) {
            send(documentNumber, errorLogConsumer, elogger);
        }
    }

    private void send(final String documentNumber, final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        FenixFramework.atomic(() -> {
            final SapRequest sapRequest = SapRoot.getInstance().getSapRequestSet().stream()
                    .filter(sr -> sr.getDocumentNumber().equals(documentNumber))
                    .findAny().get();
            final SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
            sapEvent.processPendingRequests(sapRequest, errorLogConsumer, elogger);
        });
    }
}
