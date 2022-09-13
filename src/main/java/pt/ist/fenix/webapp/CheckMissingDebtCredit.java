package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.FixedAmountInterestExemption;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class CheckMissingDebtCredit extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Documentos");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequest().contains("2021/2022"))
                .map(SapRequest::getEvent)
                .forEach(event -> check(event, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("dividas_por_lancar_2021_2022.xls", baos.toByteArray());
    }

    private void check(final Event event, final Spreadsheet spreadsheet) {
        final Money amount = event.getExemptionsSet().stream()
                .filter(exemption -> !(exemption instanceof FixedAmountInterestExemption))
                .filter(exemption -> exemption.getWhenCreated().getYear() == 2021)
                .map(exemption -> exemption.getExemptionAmount(null))
                .reduce(Money.ZERO, Money::add);
        if (amount.isPositive()) {
            if (!hasRequestType(event, SapRequestType.DEBT_CREDIT)) {
                report(event, spreadsheet, amount);
            } else {
                final Money creditDebt = event.getSapRequestSet().stream()
                        .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                        .filter(sr -> sr.getRequest().contains("2021/2022"))
                        .map(sr -> sr.getValue())
                        .reduce(Money.ZERO, Money::add);
                final Money exemptionAmount = event.getExemptionsSet().stream()
                        .filter(exemption -> !(exemption instanceof FixedAmountInterestExemption))
                        .map(exemption -> exemption.getExemptionAmount(null))
                        .reduce(Money.ZERO, Money::add);
                if (creditDebt.compareTo(exemptionAmount) != 0) {
                    taskLog("Falta só parte: %s%n", event.getExternalId());
                }
            }
        }
    }

    private boolean hasRequestType(final Event event, final SapRequestType requestType) {
        return event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .anyMatch(sr -> sr.getRequestType() == requestType);
    }

    private void report(final Event event, final Spreadsheet spreadsheet, final Money exemptionAmount) {
        final Spreadsheet.Row row = spreadsheet.addRow();

        final Money creditAmount = event.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .map(sr -> sr.getValue())
                .reduce(Money.ZERO, Money::add);
        row.setCell("Valor Isenção", exemptionAmount.toString());
        row.setCell("Valor NA", creditAmount.toString());
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + event.getExternalId());
    }
}
