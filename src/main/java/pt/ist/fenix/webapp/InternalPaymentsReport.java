package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

public class InternalPaymentsReport extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final PaymentMethod internalPaymentMethod = Bennu.getInstance().getInternalPaymentMethod();
        final Spreadsheet spreadsheet = new Spreadsheet("Imputações Internas");
        
        Bennu.getInstance().getAccountingTransactionsSet().stream()
            .filter(tx -> tx.getPaymentMethod() == internalPaymentMethod)
            .filter(tx -> !tx.isAdjustingTransaction() && !tx.hasBeenAdjusted())
            .forEach(tx -> report(tx, spreadsheet));  
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Imputações Internas.xls", baos.toByteArray());
    }

    public void report(final AccountingTransaction tx, final Spreadsheet spreadsheet) {
        final Row row = spreadsheet.addRow();
        tx.getSapRequestSet().forEach(sr -> {
            row.setCell("EventoID", tx.getEvent().getExternalId());
            row.setCell("Evento", tx.getEvent().getDescription().toString());
            row.setCell("Valor Pagamento", tx.getAmountWithAdjustment().toPlainString());
            row.setCell("Data pagamento", tx.getWhenRegistered().toString("dd/MM/yyyy HH:ss"));
            row.setCell("Imputação", tx.getComments());
            row.setCell("Cancelado", tx.getEvent().isCancelled() ? "Sim" : "Não");
            row.setCell("Nº Sap Fénix", sr.getDocumentNumber());
            row.setCell("Nº Sap", sr.getSapDocumentNumber());
            row.setCell("Data SAP", sr.getDocumentDate().toString("dd/MM/yyyy HH:ss"));
            row.setCell("Integrado", sr.getIntegrated() ? "Sim" : "Não");
            row.setCell("Ignorado", sr.getIgnore() ? "Sim" : "Não");
            row.setCell("Valor Sap", sr.getValue() != null ? sr.getValue().toPlainString() : "0");
            row.setCell("Valor Adiantamento Sap", sr.getAdvancement() != null ? sr.getAdvancement().toPlainString() : "0");
            row.setCell("Link Fénix", "https://fenix.tecnico.ulisboa.pt/accounting-management/" + tx.getEvent().getExternalId() + "/details");
        });
        if (tx.getSapRequestSet().isEmpty()) {
            row.setCell("EventoID", tx.getEvent().getExternalId());
            row.setCell("Evento", tx.getEvent().getDescription().toString());
            row.setCell("Valor Pagamento", tx.getAmountWithAdjustment().toPlainString());
            row.setCell("Data pagamento", tx.getWhenRegistered().toString("dd/MM/yyyy HH:ss"));
            row.setCell("Imputação", tx.getComments());
            row.setCell("Cancelado", tx.getEvent().isCancelled() ? "Sim" : "Não");
            row.setCell("Nº Sap Fénix", "sem info SAP");
            row.setCell("Nº Sap", "sem info SAP");
            row.setCell("Data SAP", "sem info SAP");
            row.setCell("Integrado", "sem info SAP");
            row.setCell("Ignorado", "sem info SAP");
            row.setCell("Valor Sap", "sem info SAP");
            row.setCell("Valor Adiantamento Sap", "sem info SAP");
            row.setCell("Link Fénix", "https://fenix.tecnico.ulisboa.pt/accounting-management/" + tx.getEvent().getExternalId() + "/details");
        }
    }
}
