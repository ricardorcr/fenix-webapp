package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

public class FixDateInAnulledRequests extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getSapRoot().getSapRequestSet().parallelStream().forEach(sr -> process(sr));
    }

    private void process(final SapRequest sr) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {

                @Override
                public Void call() {
                    fix(sr);
                    return null;
                }
            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void fix(final SapRequest sr) {
        if (sr.getOriginalRequest() != null && !sr.getIntegrated()) {
            if (sr.getRequestType() == SapRequestType.PAYMENT_INTEREST || sr.getRequestType() == SapRequestType.PAYMENT) {
                final JsonObject data = sr.getRequestAsJson();
                final JsonObject paymentDocument = data.getAsJsonObject("paymentDocument");
                if (paymentDocument.get("paymentDate").getAsString().contains("2019")) {
                    taskLog("Estorno %s tinha %s e vai passar a ter %s%n", sr.getExternalId(),
                            paymentDocument.get("paymentDate").getAsString(), "2018-12-31 18:36:42");
                    paymentDocument.addProperty("paymentDate", "2018-12-31 18:36:42");
                    sr.setRequest(data.toString());
                }
            }
        }
    }
}
