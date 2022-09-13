package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.checkerframework.checker.units.qual.A;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class ReportDebtCredits extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Créditos à dívida");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .forEach(sr -> report(sr, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("creditos_divida_2021.xls", baos.toByteArray());
    }

    private void report(final SapRequest request, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Nº Documento", request.getDocumentNumber());
        row.setCell("Valor", request.getValue().toString());
        final String ngNumber = getDocumentNumberForType(request, "NG");
        row.setCell("Dívida", ngNumber);
        final SapRequest debtRequest = getDebtRequest(request, ngNumber);
        row.setCell("Valor Dívida", debtRequest.getValue().toString());
        row.setCell("Ano Dívida", getDebtYear(debtRequest));
        row.setCell("IstID", request.getEvent().getPerson().getUsername());
        row.setCell("Nome", request.getEvent().getPerson().getName());
        row.setCell("Ignored", String.valueOf(request.getIgnore()));
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + request.getEvent().getExternalId());
    }

    private String getDebtYear(final SapRequest debtRequest) {
        final JsonObject workingDocument = debtRequest.getRequestAsJson().get("workingDocument").getAsJsonObject();
        final String metadataString = workingDocument.get("metadata").getAsString().replace("\\", "");
        final JsonObject metadata = new JsonParser().parse(metadataString).getAsJsonObject();
        final String startDate = metadata.get("START_DATE").getAsString();
        return startDate.substring(0, 4);
    }

    private SapRequest getDebtRequest(final SapRequest request, final String ngNumber) {
        return request.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(ngNumber))
                .findAny().get();
    }

    public String getDocumentNumberForType(final SapRequest sapRequest, final String typeCode){
        final JsonObject json = sapRequest.getRequestAsJson();
        final JsonElement paymentDocument = json.get("paymentDocument");
        if (paymentDocument != null && !paymentDocument.isJsonNull()) {
            final JsonObject paymentJson = paymentDocument.getAsJsonObject();
            final String paymentDocumentNumber = getDocumentNumber(paymentJson, "paymentDocumentNumber", typeCode);
            if(paymentDocumentNumber != null) {
                return paymentDocumentNumber;
            }
            final String workingDocumentNumber = getDocumentNumber(paymentJson, "workingDocumentNumber", typeCode);
            if (workingDocumentNumber != null) {
                return workingDocumentNumber;
            }
            final String originatingOnDocumentNumber = getDocumentNumber(paymentJson, "originatingOnDocumentNumber", typeCode);
            if (originatingOnDocumentNumber != null) {
                return originatingOnDocumentNumber;
            }
            final String paymentOriginDocNumber = getDocumentNumber(paymentJson, "paymentOriginDocNumber", typeCode);
            if (paymentOriginDocNumber != null) {
                return paymentOriginDocNumber;
            }
        }
        final JsonElement workingDocument = json.get("workingDocument");
        if (workingDocument != null && !workingDocument.isJsonNull()) {
            final JsonObject workingJson = workingDocument.getAsJsonObject();
            final String paymentOriginDocNumber = getDocumentNumber(workingJson, "paymentOriginDocNumber", typeCode);
            if (paymentOriginDocNumber != null) {
                return paymentOriginDocNumber;
            }
            final String workingDocumentNumber = getDocumentNumber(workingJson, "workingDocumentNumber", typeCode);
            if (workingDocumentNumber != null) {
                return workingDocumentNumber;
            }
            final String paymentDocumentNumber = getDocumentNumber(workingJson, "paymentDocumentNumber", typeCode);
            if(paymentDocumentNumber != null) {
                return paymentDocumentNumber;
            }
            final String workOriginDocNumber = getDocumentNumber(workingJson, "workOriginDocNumber", typeCode);
            if (workOriginDocNumber != null) {
                return workOriginDocNumber;
            }
        }
        return null;
    }

    private String getDocumentNumber(final JsonObject json, final String key, final String value){
        final JsonElement jsonElement = json.get(key);
        if(jsonElement != null && !jsonElement.isJsonNull() && jsonElement.getAsString().startsWith(value)){
            return jsonElement.getAsString();
        }
        return null;
    }
}
