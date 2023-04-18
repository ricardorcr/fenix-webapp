package pt.ist.fenix.webapp;

import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.Utils;

import java.io.ByteArrayOutputStream;

public class CheckTuitionInvoiceWithoutDebt extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Missing Debts");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getRequestAsJson().get("productDescription").getAsString().contains("PROPINAS"))
                .map(sr -> sr.getEvent())
                .filter(this::isMissing)
                .forEach(event -> report(event, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("propinas_sem_divida.xlsx", baos.toByteArray());
    }

    private void report(final Event event, final Spreadsheet spreadsheet) {
        final Money valueFaturas = calc(event, SapRequestType.INVOICE)
                            .subtract(calc(event, SapRequestType.CREDIT))
                            .subtract(calcPayment(event, SapRequestType.PAYMENT))
                            .subtract(calcPayment(event, SapRequestType.ADVANCEMENT));

        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Diferença", valueFaturas.toString());
        row.setCell("Ano", Utils.executionYearOf(event).getQualifiedName());
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + event.getExternalId());
    }

    private boolean isMissing(final Event event) {
        return !event.getSapRequestSet().stream()
                .anyMatch(sr -> sr.getRequestType() == SapRequestType.DEBT);
    }

    private Money calc(final Event event, final SapRequestType sapRequestType) {
        return event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == sapRequestType)
                .filter(sr -> !sr.getRequestAsJson().get("productCode").getAsString().equals("0063"))
                .map(sr -> sr.getValue())
                .reduce(Money.ZERO, Money::add);
    }

    private Money calcPayment(final Event event, final SapRequestType sapRequestType) {
        return event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == sapRequestType)
                .filter(sr -> !sr.getRequestAsJson().get("productCode").getAsString().equals("0063"))
                .filter(sr -> sr.getPayment() != null && !sr.getPayment().getPaymentMethod().getCode().equals("ZA"))
                .map(sr -> sr.getValue())
                .reduce(Money.ZERO, Money::add);
    }
}
