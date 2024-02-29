package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompareFenixSAPDocuments extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final String path = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/sap_documents_2023.csv";
        final Map<String, Money> map = new HashMap<>();
        try {
            final List<String> allLines = Files.readAllLines(new File(path).toPath());
            for (String line : allLines) {
                final String[] split = line.split("\t");
                final String docNumber = split[0];
                final Money value = new Money(split[3]).abs();
                map.put(docNumber, value);
            }
        } catch (Exception e) {
            throw new Error("Error reading AFS file");
        }

        final Spreadsheet spreadsheet = new Spreadsheet("Falta em SAP");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getDocumentDate().getYear() == 2023)
                .filter(sr -> sr.getEvent().isGratuity())
                .filter(sr -> sr.getRequestType() != SapRequestType.PAYMENT && sr.getRequestType() != SapRequestType.PAYMENT_INTEREST
                        && sr.getRequestType() != SapRequestType.CLOSE_INVOICE
                        && sr.getRequestType() != SapRequestType.INVOICE_INTEREST)
                .filter(sr -> sr.getOriginalRequest() == null)
                .filter(sr -> !(sr.getRequestType() == SapRequestType.CREDIT && sr.getDocumentNumber().startsWith("NR")))
                .filter(sr -> {
                    if (sr.getRequestType() == SapRequestType.ADVANCEMENT) {
                        return !map.containsKey(sr.getDocumentNumberForType("NA"));
                    } else if (sr.getRequestType() == SapRequestType.REIMBURSEMENT) {
                        return !map.containsKey(sr.getDocumentNumberForType("NA"));
                    } else {
                        return !map.containsKey(sr.getDocumentNumber());
                    }
                })
                .forEach(sr -> report(sr, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("documentos_falta_sap_2023.xlsx", baos.toByteArray());
    }

    private void report(final SapRequest request, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Nº Documento", request.getDocumentNumber());
        row.setCell("Nº Complementar", request.getDocumentNumberForType("NA"));
        row.setCell("Tipo", request.getRequestType().toString());
        row.setCell("Estornado", request.getAnulledRequest() != null ? "Sim" : "Não");
        row.setCell("Valor", request.getValue().add(request.getAdvancement()).toString());
        row.setCell("Data Documento", request.getDocumentDate().toString("dd-MM-yyyy"));
        row.setCell("Data Envio", request.getWhenSent() != null ? request.getWhenSent().toString("dd-MM-yyyy") : "");
        row.setCell("Descrição", request.getEvent().getDescriptionI18N().getContent());
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + request.getEvent().getExternalId());
    }
}
