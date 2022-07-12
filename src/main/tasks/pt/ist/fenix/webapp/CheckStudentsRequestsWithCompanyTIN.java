package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class CheckStudentsRequestsWithCompanyTIN extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Eventos");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.openInvoiceValue().isPositive())
                .filter(this::hasTINProblem)
                .forEach(sr -> report(spreadsheet, sr));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("problemas_nifs_empresas.xls", baos.toByteArray());
    }

    private boolean hasTINProblem(final SapRequest invoiceRequest) {
        final SapRequest.ClientData clientData = invoiceRequest.getClientData();
        final String tin = clientData.getVatNumber();
        final String accountID = clientData.getAccountId();
        if (accountID.equals("STUDENT")) {
            return !isPersonalNumber(tin);
        }
        return false;
    }

    private void report(final Spreadsheet spreadsheet, final SapRequest sr) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        final SapRequest.ClientData clientData = sr.getClientData();
        row.setCell("Documento", sr.getDocumentNumber());
        row.setCell("Nome", clientData.getCompanyName());
        row.setCell("NIF", clientData.getVatNumber());
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + sr.getEvent().getExternalId());
    }

    private boolean isPersonalNumber(final String tin) {
        if (tin != null && "PT".equals(tin.substring(0, 2))) {
            final String number = tin.substring(2);
            if (allNinesAndZeros(number)) {
                return false;
            }
            for (int i = 0; i < number.length(); i++) {
                final char c = number.charAt(i);
                final char nc = i + 1 < number.length() ? number.charAt(i + 1) : 'X';
                if (c == '0') {
                    // skip;
                } else if (c == '1' || c == '2' || c == '3') {
                    return true;
                } else if (c == '4' && nc == '5') {
                    return true;
                } else if (c == '7' && (nc == '0' || nc == '4' || nc == '5')) {
                    return true;
                } else if (c == '7' && nc == '7') {
                    return true;
                } else if (c == '7' && nc == '8') {
                    return true;
                } else if (c == '9' && nc == '8') {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    private boolean allNinesAndZeros(final String number) {
        for (int i = 0; i < number.length(); i++) {
            final char c = number.charAt(i);
            if (c != '0' && c != '9') {
                return false;
            }
        }
        return true;
    }
}