package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class ProcessEvent extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {

        final Spreadsheet errors = new Spreadsheet("Errors");
        final ErrorLogConsumer errorLogConsumer = new ErrorLogConsumer() {

            @Override
            public void accept(final String oid, final String user, final String name, final String amount,
                    final String cycleType, final String error, final String args, final String type,
                    final String countryOfVatNumber, final String vatNumber, final String address, final String locality,
                    final String postCode, final String countryOfAddress, final String paymentMethod, final String documentNumber,
                    final String action) {

                final Row row = errors.addRow();
                row.setCell("OID", oid);
                row.setCell("user", user);
                row.setCell("name", name);
                row.setCell("amount", amount);
                row.setCell("cycleType", cycleType);
                row.setCell("error", error);
                row.setCell("args", args);
                row.setCell("type", type);
                row.setCell("countryOfVatNumber", countryOfVatNumber);
                row.setCell("vatNumber", vatNumber);
                row.setCell("address", address);
                row.setCell("locality", locality);
                row.setCell("postCode", postCode);
                row.setCell("countryOfAddress", countryOfAddress);
                row.setCell("paymentMethod", paymentMethod);
                row.setCell("documentNumber", documentNumber);
                row.setCell("action", action);

            }
        };
        final EventLogger elogger = (msg, args) -> taskLog(msg, args);

        //process("1970350606779044", errorLogConsumer, elogger);

//        final File file = new File("/home/rcro/sap/erros_noValidPostCode.txt");
//        List<String> allLines = Files.readAllLines(file.toPath());

        Bennu.getInstance().getAccountingEventsSet().stream().filter(e -> !e.getSapRequest().isEmpty())
                .flatMap(e -> e.getSapRequestSet().stream()).filter(sr -> !sr.getIntegrated())
                .forEach(sr -> process(sr.getEvent(), errorLogConsumer, elogger));

//      allLines.stream().forEach(l -> {
//            process(l, errorLogConsumer, elogger);
//        });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            errors.exportToCSV(stream, "\t");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        output("SapErrorLog.xls", stream.toByteArray());
    }

    private void process(String eventOID, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        Event event = FenixFramework.getDomainObject(eventOID);

        if (Utils.validate(errorLogConsumer, event)) {
            EventProcessor.syncEventWithSap(errorLogConsumer, elogger, event);
        }
    }

    private void process(Event event, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        if (Utils.validate(errorLogConsumer, event)) {
            EventProcessor.syncEventWithSap(errorLogConsumer, elogger, event);
        }
    }
}
