package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class CheckAdvancementUses extends CustomTask {

    public void runTask() throws IOException {
        File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/adiantamentos_movimentos_talvez_errados.txt");
        final List<String> lines = Files.readAllLines(file.toPath());
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> lines.contains(sr.getDocumentNumber()))
                .forEach(this::report);
    }

    private void report(final SapRequest sapRequest) {
        JsonObject paymentDocument = sapRequest.getRequestAsJson().get("paymentDocument").getAsJsonObject();
        JsonArray documents = paymentDocument.get("documents").getAsJsonArray();
        taskLog("%s\t%s%n", sapRequest.getDocumentNumber(), documents.get(0).getAsJsonObject().get("originDocNumber"));
    }
}