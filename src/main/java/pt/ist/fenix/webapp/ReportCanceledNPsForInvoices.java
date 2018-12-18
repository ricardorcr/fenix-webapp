package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.Atomic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class ReportCanceledNPsForInvoices extends CustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    private static final Spreadsheet sheet = new Spreadsheet("NPs Estornados");

    @Override
    public void runTask() throws Exception {
        File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/facturas_com_NP_estornados.txt");
        Set<String> invoiceNumbers = new HashSet<>(Files.readAllLines(file.toPath()));

        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE || sr.getRequestType() == SapRequestType.INVOICE_INTEREST)
                .filter(sr -> invoiceNumbers.contains(sr.getDocumentNumber()))
                .forEach(sr -> {
                    sr.getEvent().getSapRequestSet().stream()
                            .filter(osr -> osr.getRequestType() == SapRequestType.PAYMENT || osr.getRequestType() == SapRequestType.ADVANCEMENT
                                    || osr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                            .filter(osr -> osr.getAnulledRequest() != null && osr.getAnulledRequest().getIntegrated())
                            .filter(osr -> osr.refersToDocument(sr.getDocumentNumber()))
                            .forEach(this::report);
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sheet.exportToXLSSheet(stream);
        output("NPsEstornados.xls", stream.toByteArray());
    }

    private void report(final SapRequest sapRequest) {
        final Spreadsheet.Row row = sheet.addRow();
        row.setCell("Factura", sapRequest.getDocumentNumberForType("ND"));
        row.setCell("Pagamento", sapRequest.getDocumentNumber());
        row.setCell("Documento SAP", sapRequest.getSapDocumentNumber());
        row.setCell("Data estorno", sapRequest.getAnulledRequest().getWhenSent().toString("dd-MM-yyyy HH:mm"));
    }
}
