package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class CheckWrongExemptionCodeSAP extends CustomTask {
    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Documentos");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() != SapRequestType.INVOICE && sr.getRequestType() != SapRequestType.DEBT && sr.getRequestType() != SapRequestType.DEBT_CREDIT)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getAnulledRequest() == null && sr.getOriginalRequest() == null)
                //.filter(sr -> sr.getWhenCreated().getYear() >= 2022 && sr.getWhenCreated().getMonthOfYear() >=5)
                .filter(this::isWrong)
                .forEach(sr -> report(spreadsheet, sr));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("documentos_codigo_errado.xls", baos.toByteArray());
    }

    private void report(final Spreadsheet spreadsheet, final SapRequest sr) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Nº Documento", sr.getDocumentNumber());
        row.setCell("Nº Documento SAP", sr.getSapDocumentNumber());
        row.setCell("Tipo", sr.getRequestType().toString());
        row.setCell("Valor", sr.getValue().toString());
        row.setCell("Data envio", sr.getWhenSent() != null ? sr.getWhenSent().toString("dd/MM/yyyy") : "");
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + sr.getEvent().getExternalId());
    }

    private boolean isWrong(final SapRequest sapRequest) {
        final String productCode = sapRequest.getRequestAsJson().get("productCode").getAsString();
        if (productCode.equals("0063")) {
            final String invoiceNumber = sapRequest.getDocumentNumberForType("ND");
            try {
                final String invoiceProductCode = sapRequest.getEvent().getSapRequestSet().stream()
                        .filter(sr -> sr.getDocumentNumber().equals(invoiceNumber))
                        .map(sr -> sr.getRequestAsJson().get("productCode").getAsString())
                        .findAny().get();
                if (!productCode.equals(invoiceProductCode)) {
                    return true;
                }
            } catch (Exception e) {
                taskLog("Documento: %s\tEvento: %s%n", sapRequest.getDocumentNumber(), sapRequest.getEvent().getExternalId());
            }
        }
        return false;
    }
}