package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.smartFlow.domain.Flow;
import pt.ist.fenixframework.FenixFramework;

public class CheckApplicationsFlowStateForProcess extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("852907490541718"); //DGES 1ª fase 24/25
        Spreadsheet spreadsheet = new Spreadsheet("Estado candidaturas");
        admissionProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> Utils.outcomeStateFor(app) != RegistrationProcessState.CONFIRMED)
                .forEach(app -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("AppID", app.getExternalId());
                    row.setCell("IstID", app.getAccount().getUsername());
                    String name = "";
                    if (app.getAccount().getIdentity() != null && app.getAccount().getIdentity().getPersonalInformation() != null) {
                        name = app.getAccount().getIdentity().getPersonalInformation().getDisplayName();
                    }
                    row.setCell("Nome", name);
                    final Enum outcomeState = Utils.outcomeStateFor(app);
                    row.setCell("Estado", outcomeState != null ? outcomeState.name() : "");
                    String flowState = "";
                    if (app.getDataObject().has("flow")) {
                        final Flow flow = FenixFramework.getDomainObject(app.getDataObject().get("flow").getAsString());
                        flowState = flow.getCurrentFlowNode() != null ? flow.getCurrentFlowNode().getNodeName() : "";
                    }
                    row.setCell("Estado Flow", flowState);
                });
        output("estado_candidaturas.xlsx", spreadsheet.exportToXLSXSheet());
    }
}
