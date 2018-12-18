package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.util.Money;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class FixAdvancementUses extends SapCustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    protected void runTask(final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
//        File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/uso_adiantamentos_mudar_NL.txt");
//        File file = new File("/home/rcro/DocumentsSSD/fenix/sap/uso_adiantamentos_mudar_NL.txt");
//        try {
//            final List<String> lines = Files.readAllLines(file.toPath());
//        SapRoot.getInstance().getSapRequestSet().stream()
//                .filter(sr -> !sr.isInitialization())
//                .filter(sr -> lines.contains(sr.getDocumentNumber()))
//                .forEach(sr -> fix(sr, errorLogConsumer, elogger));
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new Error("Erro a ler o ficheiro");
//        }

        SapRequest sapRequest = FenixFramework.getDomainObject("1978558289439268"); //NP209964 -> 1º da lista
        fix(sapRequest, errorLogConsumer, elogger);
    }

    private void fix(final SapRequest sapRequest, final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        FenixFramework.atomic(() -> {
            JsonObject requestAsJson = sapRequest.getRequestAsJson();
            JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
            paymentDocument.addProperty("settlementType", "NN");
            //para o valor PaymentAmount ir preenchido, este campo tem que ir a zeros, normalmente vai com o valor negativo
            //para ir a zeros, tal como foi dito que era suposto ser no uso dos adiantamentos
            paymentDocument.addProperty("excessPayment", Money.ZERO.toPlainString());
            sapRequest.setRequest(requestAsJson.toString());
            sapRequest.setSent(false);
            sapRequest.setIntegrated(false);
            if (sapRequest.getSapDocumentFile() != null) {
                sapRequest.getSapDocumentFile().delete();
            }
            SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
            sapEvent.processPendingRequests(sapRequest, errorLogConsumer, elogger);
        });
    }
}