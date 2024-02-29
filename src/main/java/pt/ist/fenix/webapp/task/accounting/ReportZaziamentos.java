package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class ReportZaziamentos extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Zaziamentos");
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                        .filter(tx -> tx.getPaymentMethod().getCode().equals("ZA"))
                                .forEach(tx -> report(tx, spreadsheet));
//        SapRoot.getInstance().getSapRequestSet().stream()
//                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
//                .filter(sr -> !sr.isInitialization())
//                .filter(sr -> sr.getPayment() != null)
//                .forEach(sr -> report(sr, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("imputacoes_internas.xlsx", baos.toByteArray());
    }

    private void report(final AccountingTransactionDetail transactionDetail, final Spreadsheet spreadsheet) {
        transactionDetail.getTransaction().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .forEach(sr -> report(sr, spreadsheet));
    }

    private void report(final SapRequest sapRequest, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("RequestOID", sapRequest.getExternalId());
        row.setCell("Nº Documento", sapRequest.getDocumentNumber());
        row.setCell("Valor", sapRequest.getValue().toString());
        row.setCell("Data Doc", sapRequest.getDocumentDate().toString("dd-MM-yyyy"));
        row.setCell("Data Envio", sapRequest.getWhenSent().toString("dd-MM-yyyy"));
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + sapRequest.getEvent().getExternalId());
    }
}
