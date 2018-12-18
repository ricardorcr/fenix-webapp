package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class CheckSapRequests2020 extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Missing 2020 to send");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.getIntegrated())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getDocumentDate().getYear() == 2020)
                .forEach(sr -> report(sr, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Documentos_por_enviar_2020.xls", baos.toByteArray());
    }

    private void report(SapRequest sr, Spreadsheet spreadsheet) {
        Row row = spreadsheet.addRow();
        row.setCell("Event ID",sr.getEvent().getExternalId());
        row.setCell("Doc Number",sr.getDocumentNumber());
        row.setCell("Request ID",sr.getExternalId());
        row.setCell("Tipo",sr.getRequestType().toString());
        row.setCell("Data doc",sr.getDocumentDate().toString("dd-MM-yyyy HH:mm:ss"));
    }
}
