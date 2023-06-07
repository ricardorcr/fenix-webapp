package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.IBAN;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class ReportDedicatedIBANs extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("IBANs");
        Bennu.getInstance().getIBANGroupSet().stream()
                .flatMap(group -> group.getIBANSet().stream())
                .forEach(iban -> report(iban, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("relatorio_ibans_dedicados.xlsx", baos.toByteArray());
    }

    private void report(final IBAN iban, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("ID", iban.getExternalId());
        row.setCell("IBAN", iban.getIBANNumber());
        final Person person = iban.getEvent().getPerson();
        row.setCell("Nacionalidade", person.getCountry().getName());
        row.setCell("País NIF", person.getVATCountry() == null ? "N/A" : person.getVATCountry().getName());
        row.setCell("Pago", iban.getIBANPaymentSet().isEmpty() ? "Não" : "Sim");
    }
}
