package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import org.fenixedu.academic.domain.space.SpaceUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.fenixedu.spaces.domain.Space;

import com.google.common.base.Strings;

public class ReportSpacesWithIds extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Spaces");
        SpaceUtils.allocatableSpaces()
            .filter(s -> !Strings.isNullOrEmpty(s.getName()))
            .forEach(s -> report(s, spreadsheet));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Espacos.xls", baos.toByteArray());
    }

    private void report(Space space, Spreadsheet spreadsheet) {
        final Row row = spreadsheet.addRow();
        row.setCell("OID", space.getExternalId());
        row.setCell("Nome", space.getName());
        row.setCell("Nome/Caminho", space.getPresentationName());
        row.setCell("Tipo", space.getClassification().getName().getContent());
        Optional<Integer> examCapacity = space.getMetadata("examCapacity");
        row.setCell("Capacidade Exame", examCapacity.isPresent() ? examCapacity.get() : 0);
    }
}
