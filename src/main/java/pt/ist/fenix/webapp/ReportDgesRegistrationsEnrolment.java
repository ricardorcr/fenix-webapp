package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.integration.domain.student.importation.DegreeCandidateDTO;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;

public class ReportDgesRegistrationsEnrolment extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Inscrições Dges 2021 Fase2");

        final AdmissionProcess process = FenixFramework.getDomainObject("571432513830913");
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> app.getDataObject().has("registration"))
                .forEach(app -> report(app, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("inscricoes_dges_fase2.xls", baos.toByteArray());
    }

    private void report(final Application app, final Spreadsheet spreadsheet) {
        final String registrationOID = app.getDataObject().get("registration").getAsString();
        final Registration registration = FenixFramework.getDomainObject(registrationOID);
        final RegistrationState registrationState = registration.getActiveState();
        final Spreadsheet.Row row = spreadsheet.addRow();

        row.setCell("Curso",registration.getDegreeCurricularPlanName());
        row.setCell("IstID", registration.getPerson().getUsername());
        row.setCell("Inscrições?", registration.getEnrolments(registration.getRegistrationYear()).size() > 0 ? "Sim" : "Não");
        row.setCell("Nome", registration.getPerson().getName());
        row.setCell("Data", registrationState.getStateDate().toString("dd/MM/yyyy HH:mm"));
        row.setCell("Estado", registrationState.getStateType().getDescription());
    }
}
