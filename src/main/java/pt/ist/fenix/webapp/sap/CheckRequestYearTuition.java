package pt.ist.fenix.webapp.sap;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.Utils;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CheckRequestYearTuition extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final List<String> docNumbers = Arrays.asList("ND639100", "ND651849", "ND653967", "ND654843", "ND656096", "ND656100", "ND656104", "ND686308", "ND686310", "ND686312", "ND686910", "ND689269", "ND691434", "ND694582", "NA697226", "NA697230", "ND697217", "ND697219", "ND697221", "ND697223", "ND697227", "ND697235", "ND703525", "ND703527", "ND710180", "ND713803", "ND736125", "ND736603", "ND736342", "ND736346", "ND736350", "ND736599", "ND737443", "ND740320", "ND754009", "ND740774", "ND741185", "ND744262", "ND744266", "ND744584", "ND744580", "ND744653", "ND748116", "ND748697", "ND748699", "ND748703", "ND748727", "ND748731", "ND748735", "ND748739", "ND748743", "ND752624", "ND752626", "ND752905", "ND753811", "ND754929", "ND754931", "ND754935", "ND755150", "ND755144", "ND755148", "ND755152", "ND810974", "ND810976", "ND810978", "ND756202", "ND756205", "ND756207", "ND756209", "ND756211", "ND756213", "ND756215", "ND756217", "ND814929", "ND814927", "ND830024", "ND835070", "ND835074", "ND837225", "ND842858", "ND845590", "ND861525", "ND871555", "ND861937", "ND890372", "ND869452", "NA888455", "ND888454", "ND872194", "ND890093", "ND890096", "ND890101", "ND890104", "ND901000", "ND890090", "ND900999", "ND869754", "ND890368", "ND890369", "ND890370", "ND901021", "ND890363", "ND870573", "ND885324", "ND886563", "ND890386", "ND891967", "ND886661", "ND889356", "ND891885", "ND890574", "ND892468", "ND889280", "ND889282", "ND889284", "ND892661", "ND892772", "ND892825", "ND892563", "ND894399", "ND894402", "ND894409", "ND895694", "ND893493", "ND896049", "ND896514", "ND894438", "NA901656", "NA901658", "NA901660", "NA901664", "NA901666", "NA901668", "NA901670", "NA901674", "NA901688", "NA901940", "NA901942", "NA901944", "NA901948");

        final Spreadsheet spreadsheet = new Spreadsheet("Documentos");
        docNumbers.stream()
                .forEach(docNumber -> report(getSapRequest(docNumber), spreadsheet));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("anos_lectivos.xlsx", baos.toByteArray());
    }

    private void report(final SapRequest sapRequest, final Spreadsheet spreadsheet) {
        if (sapRequest == null) return;;
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Documento", sapRequest.getDocumentNumber());
        row.setCell("Ano", Utils.executionYearOf(sapRequest.getEvent()).getQualifiedName());
        row.setCell("Propina", String.valueOf(sapRequest.getEvent().isGratuity()));
        row.setCell("Event", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + sapRequest.getEvent().getExternalId());
    }

    private SapRequest getSapRequest(final String docNumber) {
        try {

            final Optional<SapRequest> sapRequest = SapRoot.getInstance().getSapRequestSet().stream()
                    .filter(sr -> sr.getDocumentNumber().equals(docNumber))
                    .findAny();
            if (sapRequest.isPresent()) {
                return sapRequest.get();
            } else {
                taskLog("hummm %s%n", docNumber);
                return null;
            }
        } catch (Exception e) {
            taskLog("hummm %s%n", docNumber);
            throw e;
        }

    }
}
