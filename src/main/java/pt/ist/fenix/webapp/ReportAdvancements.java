package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

public class ReportAdvancements extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Adiantamentos");
        spreadsheet.setHeaders("Adiantamento NP", "Adiantamento NA", "Adiantamento SAP Doc", "Valor Adiantamento", "Utilização NP",
                "Utilização SAP", "Valor Usado", "Reembolso", "Valor Reembolsado");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getAnulledRequest() == null && sr.getOriginalRequest() == null)
                .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .forEach(sr -> report(sr, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Adiantamentos.xlsx", baos.toByteArray());
    }

    private void report(final SapRequest advancementRequest, final Spreadsheet spreadsheet) {
        // sapRequest.getReimbursementRequest()
        // mas pode ter mais do que um associado, só está a guardar 1 e devia ser uma lista tem que ser ver pelo request
        final String naDocNumber = advancementRequest.getDocumentNumberForType("NA");
        Set<SapRequest> advancementUseRequests = new HashSet<>();
        Set<SapRequest> reimbursementRequests = new HashSet<>();
        for (SapRequest sapRequest : SapRoot.getInstance().getSapRequestSet()) {
            if (!sapRequest.isInitialization() && !sapRequest.getIgnore() &&
                sapRequest.getAnulledRequest() == null && sapRequest.getOriginalRequest() == null) {
                if (sapRequest.getRequestType() == SapRequestType.PAYMENT || sapRequest.getRequestType() == SapRequestType.PAYMENT_INTEREST) {
                    if (sapRequest.getRequest().contains(naDocNumber)) {
                        advancementUseRequests.add(sapRequest);
                    }
                } else if (sapRequest.getRequestType() == SapRequestType.REIMBURSEMENT) {
                    if (sapRequest.getAdvancementRequest() == advancementRequest) {
                        reimbursementRequests.add(sapRequest);
                    }
                }
            }
        }

        for (SapRequest sapRequest : advancementUseRequests) {
            final Spreadsheet.Row row = createRow(advancementRequest, spreadsheet);
            row.setCell(4, sapRequest.getDocumentNumber());
            row.setCell(5, sapRequest.getSapDocumentNumber());
            row.setCell(6, sapRequest.getValue().toString());
        }
        for (SapRequest sapRequest : reimbursementRequests) {
            final Spreadsheet.Row row = createRow(advancementRequest, spreadsheet);
            row.setCell(7, sapRequest.getDocumentNumber());
            row.setCell(8, sapRequest.getValue().toString());
        }

        if (advancementUseRequests.isEmpty() && reimbursementRequests.isEmpty()) {
            createRow(advancementRequest, spreadsheet);
        }
    }

    private Spreadsheet.Row createRow(final SapRequest advancementRequest, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell(0, advancementRequest.getDocumentNumber());
        row.setCell(1, advancementRequest.getDocumentNumberForType("NA"));
        row.setCell(2, advancementRequest.getSapDocumentNumber());
        row.setCell(3, advancementRequest.getAdvancement().toString());
        return row;
    }
}