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
            documentNumbers = Arrays.asList("NJ661397","NJ661362","NJ661054","NJ660464","NJ660308","NJ688033","NJ687933","NJ687413","NJ686629","NJ685864",
                    "NJ685760","NJ685701","NJ685116","NJ683436","NJ682778","NJ682082","NJ680723","NJ680616","NJ678809","NJ678658","NJ677842","NJ677838",
                    "NJ676891","NJ676018","NJ675512","NJ675508","NJ675480","NJ674384","NJ674146","NJ673940","NJ673409","NJ671016","NJ670959","NJ670919",
                    "NJ669738","NJ669621","NJ669462","NJ668399","NJ668145","NJ668121","NJ667537","NJ667429","NJ667324","NJ666590","NJ664952","NJ664679",
                    "NJ664659","NJ663886","NJ663752","NJ663133","NJ704059","NJ703688","NJ703470","NJ702624","NJ702255","NJ701406","NJ701389","NJ701049",
                    "NJ700131","NJ699713","NJ698622","NJ694747","NJ694336","NJ694333","NJ693640","NJ693080","NJ692966","NJ692708","NJ691639","NJ690787",
                    "NJ690754","NJ698173","NJ697471","NJ696339","NJ628964");
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
