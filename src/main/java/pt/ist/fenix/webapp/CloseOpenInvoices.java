package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.SapEvent;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CloseOpenInvoices extends CustomTask {

    final StringBuilder advancements = new StringBuilder();
    final StringBuilder finalPayments = new StringBuilder();
    final StringBuilder finalCredits = new StringBuilder();

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getSapRoot().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization() && !sr.getIgnore() && sr.getIntegrated() &&
                        sr.getRequestType().equals(SapRequestType.INVOICE))
                .filter(sr -> !sr.openInvoiceValue().isPositive())
                .map(this::getFinalRequest)
                .filter(Objects::nonNull)
                .forEach(this::processFinalDocument);

        output("advancements_with_final_payment.csv", advancements.toString().getBytes());
        output("final_payments_cancelled.csv", finalPayments.toString().getBytes());
        output("final_credits_closed_with_zero_payment.csv", finalCredits.toString().getBytes());
    }

    private SapRequest getFinalRequest(final SapRequest sapRequest) {
        SapEvent sapEvent = new SapEvent(sapRequest.getEvent());

        Stream<SapRequest> paymentsAndCreditsFor = getPaymentsAndCreditsFor(sapEvent, sapRequest);
        if (paymentsAndCreditsFor.count() > 1) {
            SapRequest finalRequest = getPaymentsAndCreditsFor(sapEvent, sapRequest).
                    sorted(SapRequest.COMPARATOR_BY_ORDER.reversed()).findFirst().get();
            if (finalRequest.getIntegrated()) {
                return finalRequest;
            }
        }
        return null;
    }

    private void processFinalDocument(final SapRequest sapRequest) {
        SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
        if (sapRequest.getRequestType().equals(SapRequestType.PAYMENT)) {
            finalPayments.append("Vou estornar pagamento final:\t").append(sapRequest.getExternalId()).append("\t")
                    .append(sapRequest.getDocumentNumber()).append("\t").append(sapRequest.getEvent().getExternalId()).append("\n");
//            taskLog("Vou estornar pagamento final: %s %s%n", sapRequest.getDocumentNumber(), sapRequest.getEvent().getExternalId());
            String advancementNumbers = sapRequest.getPayment().getSapRequestSet().stream()
                    .filter(sr -> sr != sapRequest)
                    .filter(sr -> !sr.getIgnore())
                    .filter(sr -> sr.getRequestType().equals(SapRequestType.ADVANCEMENT))
                    .map(sr -> sr.getDocumentNumber())
                    .collect(Collectors.joining(","));
            if (!Strings.isNullOrEmpty(advancementNumbers)) {
                advancements.append(sapRequest.getEvent().getExternalId()).append("\t").append(advancementNumbers).append("\n");
            }
            //estornar, só depois de se comunicar é que se consegue calcular o novo pagamento final
            sapEvent.cancelDocument(sapRequest);
        } else { //is CREDIT
            final String invoiceNumber = getDocumentNumberForType("ND", sapRequest);
            SapRequest invoiceRequest = sapEvent.getFilteredSapRequestStream()
                    .filter(sr -> sr.getRequestType().equals(SapRequestType.INVOICE) && invoiceNumber.equals(sr.getDocumentNumber()))
                    .findAny().orElse(null);
            if (invoiceRequest == null) {
                throw new Error("Não encontrei a factura para " + sapRequest.getDocumentNumber() + " do evento " + sapRequest.getEvent().getExternalId());
            } else if (sapRequest.getRefund() == null) {
//                taskLog("Vou fechar a factura %s cujo último doc é: %s %s%n", invoiceNumber, sapRequest.getDocumentNumber(), sapRequest.getEvent().getExternalId());
//                sapEvent.registerFinalZeroPayment(invoiceRequest, creditEntry.getId());
                //TODO acrescentar id request criado
                finalCredits.append("Vou fechar a factura\t").append(invoiceNumber).append("\t").append(sapRequest.getDocumentNumber())
                        .append("\t").append(sapRequest.getEvent().getExternalId()).append("\n");
            }
        }
    }

    public String getDocumentNumberForType(String typeCode, SapRequest sr) {
        final JsonObject json = new JsonParser().parse(sr.getRequest()).getAsJsonObject();
        final JsonElement paymentDocument = json.get("paymentDocument");
        if (paymentDocument != null && !paymentDocument.isJsonNull()) {
            final JsonObject paymentJson = paymentDocument.getAsJsonObject();
            final String paymentDocumentNumber = getDocumentNumber(paymentJson, "workingDocumentNumber", typeCode);
            if (paymentDocumentNumber != null) {
                return paymentDocumentNumber;
            }
        }
        final JsonElement workingDocument = json.get("workingDocument");
        if (workingDocument != null && !workingDocument.isJsonNull()) {
            final JsonObject workingJson = workingDocument.getAsJsonObject();
            final String workOriginDocNumber = getDocumentNumber(workingJson, "workOriginDocNumber", typeCode);
            if (workOriginDocNumber != null) {
                return workOriginDocNumber;
            }
        }
        return null;
    }

    private String getDocumentNumber(final JsonObject json, final String key, final String value) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && !jsonElement.isJsonNull() && jsonElement.getAsString().startsWith(value)) {
            return jsonElement.getAsString();
        }
        return null;
    }

    private Stream<SapRequest> getPaymentsAndCreditsFor(SapEvent sapEvent, SapRequest request) {
        return sapEvent.getFilteredSapRequestStream()
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.CREDIT)
                .filter(sr -> sr.refersToDocument(request.getDocumentNumber())).sorted(SapRequest.COMPARATOR_BY_ORDER);
    }
}
