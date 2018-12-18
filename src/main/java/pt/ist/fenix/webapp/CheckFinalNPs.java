package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckFinalNPs extends CustomTask {

    @Override
    public void runTask() throws Exception {

        final Map<String, Money> npsFenix = new HashMap<>();

        GroupBasedFile file = FenixFramework.getDomainObject("282093452062114");
        BufferedReader readerFenix = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(file.getContent())));
//        BufferedReader readerFenix = new BufferedReader(new FileReader("/home/rcro/DocumentsSSD/fenix/sap/NPs_Fenix_valores_diferentes_SAP.txt"));
        String np = readerFenix.readLine();
        try {
            while (np != null) {
                String[] line = np.split("\t");
                npsFenix.put(line[0], new Money(line[1]));
                np = readerFenix.readLine();
            }
        } catch (Exception e) {
            taskLog(np);
            e.printStackTrace();
        }
        finally {
            readerFenix.close();
        }

        final Map<String, Money> extraMoney = new HashMap<>();
        npsFenix.keySet().stream().map(npNumber -> findNP(npNumber)).filter(sr -> !isFinalPayment(sr))
                .forEach(sr ->
                {
                    Money sapValue = npsFenix.get(sr.getDocumentNumber());
                    taskLog("Este malandro não é um pagamento final! %s%n", sr.getDocumentNumber(), sr.getValue(), sapValue);
                    if (sr.getIgnore()) {
                        taskLog("$$$$$$ hummmm: %s %s%n", sr.getValue(), sapValue);
                    }
                    Money extraValue = sapValue.subtract(sr.getValue());
                    extraMoney.merge("1", extraValue, (money1, money2) -> money1.add(money2));
                });

        taskLog("O Valor a mais desta brincadeira é: %s%n", extraMoney.get("1"));
    }

    private SapRequest findNP(final String npNumber) {
        return SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equalsIgnoreCase(npNumber)).findAny().orElse(null);
    }

    private boolean isFinalPayment(final SapRequest sr) {
        try {
            JsonObject paymentDocument = sr.getRequestAsJson().get("paymentDocument").getAsJsonObject();
            JsonArray documents = paymentDocument.get("documents").getAsJsonArray();
            return documents != null && documents.size() > 0;
        } catch (Exception e) {
            taskLog("################### %s%n ######################", sr.getDocumentNumber());
            e.printStackTrace();
            return false;
        }
    }
}