package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.RefundState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.sap.client.SapFinantialClient;

public class CheckReimbursementState extends CustomTask {


    @Override
    public void runTask() throws Exception {

//        JsonObject result = SapFinantialClient.getReimbursementState("sem sentido", SapEvent.IST_VAT_NUMBER, SapEvent.PROCESS_ID);
//        taskLog("%s %s\n", sr.getExternalId(), result.toString());

//        SapRequest sr = FenixFramework.getDomainObject("1415608335880669");
//        taskLog("%s %s %s ", sr.getRequestType() == SapRequestType.REIMBURSEMENT, !sr.isInitialization(), RefundState.CONCLUDED != sr.getRefundState());


//        Bennu.getInstance().getSapRoot().getSapRequestSet().stream().parallel().forEach(sr -> process(sr));
    }

    private void process(final SapRequest sr) {
        FenixFramework.atomic(() -> {
            if (!sr.isInitialization() && sr.getRequestType() == SapRequestType.REIMBURSEMENT && RefundState.CONCLUDED != sr.getRefundState()) {
                String documentNumber = sr.getDocumentNumberForType("NA");
                JsonObject result = SapFinantialClient.getReimbursementState(documentNumber, SapEvent.IST_VAT_NUMBER, SapEvent.PROCESS_ID);


                final String statusCode = result.get("statusCode").getAsString();
                if(!statusCode.equals("E")) {
                    RefundState refundState = RefundState.valueOf(statusCode);
                    String stateDateStr = result.get("statusDate").getAsString();
                    LocalDate stateDate = DateTimeFormat.forPattern("yyyy-MM-dd").parseLocalDate(stateDateStr);

                    sr.setRefundState(refundState);
                    sr.setRefundStateDate(stateDate);

                    taskLog("%s %s\n", sr.getExternalId(), result.toString());
                } else {

                }
            }
        });
    }
}
