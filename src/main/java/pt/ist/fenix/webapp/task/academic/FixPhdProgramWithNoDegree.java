package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class FixPhdProgramWithNoDegree extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Validar");

        Bennu.getInstance().getPartysSet().stream()
                .filter(Person.class::isInstance)
                .map(Person.class::cast)
                .flatMap(p -> p.getPhdIndividualProgramProcessesSet().stream())
                .filter(phd -> phd.getPhdProgram() != null)
                .filter(phd -> phd.getPhdProgram().getDegree() == null)
                .forEach(phd -> {
                    final boolean hasRegistration = phd.getRegistration() != null;
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("OID", phd.getExternalId());
                    row.setCell("Número", phd.getProcessNumber());
                    row.setCell("Estado", phd.getActiveState().name());
                    row.setCell("PhdProgram", phd.getPhdProgram().getName().getContent());
                    row.setCell("Curso Matrícula", hasRegistration ? phd.getRegistration().getDegree().getNameI18N().getContent() : "");
                    if (hasRegistration) {
                        phd.getPhdProgram().setDegree(phd.getRegistration().getDegree());
                    }
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("phdProgram_without_degree.xlsx", baos.toByteArray());
    }
}
