package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class CheckPhdExemptions extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Isenções Phd");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getWhenCreated().getYear() == 2022)
                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .filter(sr -> sr.getEvent() instanceof PhdGratuityEvent)
                .filter(sr -> sr.getRequest().contains("0063"))
                .filter(this::isFrom2021)
                .forEach(sr -> report(sr, spreadsheet));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("phdExemptions.xls", baos.toByteArray());
    }

    private boolean isFrom2021(final SapRequest sapRequest) {
        final String invoiceNumber = sapRequest.getDocumentNumberForType("ND");
        final SapRequest invoiceRequest = sapRequest.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals(invoiceNumber))
                .findAny().get();
        return invoiceRequest.getDocumentDate().getYear() == 2021;
    }

    private void report(final SapRequest sapRequest, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("EventId", sapRequest.getEvent().getExternalId());
        row.setCell("Doc Number", sapRequest.getDocumentNumber());
        row.setCell("Sap Doc Number", sapRequest.getSapDocumentNumber());
        row.setCell("When Created", sapRequest.getWhenCreated().toString());
        row.setCell("Event Created", sapRequest.getEvent().getWhenOccured().toString());
    }
}
