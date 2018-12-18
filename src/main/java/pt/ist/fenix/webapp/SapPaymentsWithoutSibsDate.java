package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;

import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.YearMonthDay;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.Atomic;

public class SapPaymentsWithoutSibsDate extends CustomTask {

    final static private boolean addSibsDate = false;

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Pagamentos sibs 2020");

        String reportName = "Pagamentos_sibs_sem_data_em_SAP_2020.xls";
        if (addSibsDate) {
            reportName = "Pagamentos_sibs_data_adicionada_2020.xls";
        }
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getSent() && sr.getWhenSent() != null && sr.getWhenSent().getYear() == 2020)
                .filter(sr -> sr.getRequestType().equals(SapRequestType.ADVANCEMENT) ||
                        sr.getRequestType().equals(SapRequestType.PAYMENT) ||
                        sr.getRequestType().equals(SapRequestType.PAYMENT_INTEREST))
                .filter(sr -> sr.getPayment() != null && sr.getPayment().getPaymentMethod() == PaymentMethod.getSibsPaymentMethod())
                .filter(sr -> sr.getDocumentDate().getYear() == 2020)
                .filter(sr -> sr.getRequest().contains("sibsDate"))
                .forEach(sr -> {
                    if (addSibsDate) {
                        JsonObject requestAsJson = sr.getRequestAsJson();
                        JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
                        addSibsMetadata(paymentDocument, sr.getPayment().getTransactionDetail());
                        sr.setRequest(requestAsJson.toString());
                        final Spreadsheet.Row row = spreadsheet.addRow();
                        row.setCell("Número Sap", sr.getSapDocumentNumber());
                        row.setCell("Número Fénix", sr.getDocumentNumber());
                        row.setCell("Evento", sr.getEvent().getExternalId());
                    } else {
                        final Spreadsheet.Row row = spreadsheet.addRow();
                        row.setCell("Número Sap", sr.getSapDocumentNumber());
                        row.setCell("Número Fénix", sr.getDocumentNumber());
                        SibsTransactionDetail sibsTx = (SibsTransactionDetail) sr.getPayment().getTransactionDetail();
                        YearMonthDay sibsDate = sibsTx.getSibsLine().getHeader().getWhenProcessedBySibs();
                        row.setCell("Data Sibs", sibsDate.toString(ISODateTimeFormat.date()));
                        row.setCell("Data Envio", sr.getWhenSent().toString(ISODateTimeFormat.dateHourMinuteSecond()));
                    }
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToCSV(stream, "\t");
        output(reportName, stream.toByteArray());
    }

    private void addSibsMetadata(final JsonObject json, final AccountingTransactionDetail transactionDetail) {
        SibsTransactionDetail sibsTx = (SibsTransactionDetail) transactionDetail;
        YearMonthDay sibsDate = sibsTx.getSibsLine().getHeader().getWhenProcessedBySibs();
        json.addProperty("sibsDate", sibsDate.toString(ISODateTimeFormat.date()));
    }
}
