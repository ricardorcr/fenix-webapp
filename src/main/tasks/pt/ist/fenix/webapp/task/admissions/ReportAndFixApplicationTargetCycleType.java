package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;

public class ReportAndFixApplicationTargetCycleType extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("AdmissionsCycleCheck");
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(Utils::isDegreeType)
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .forEach(target -> {
                    final JsonObject outcome = target.getOutcomeConfigJson();
                    final Degree degree = FenixFramework.getDomainObject(outcome.get("degree").getAsString());
                    final CycleType cycleType = cycleType(outcome);
                    boolean ok = true;
                    if (cycleType != null) {
                        ok = degree.getCycleTypes().contains(cycleType);
                    }
                    spreadsheet.addRow()
                            .setCell("Process", target.getAdmissionProcess().getTitle().getContent())
                            .setCell("Target", target.getName().getContent())
                            .setCell("Degree", degree.getSigla())
                            .setCell("Cycle", cycleType == null ? "" : cycleType.name())
                            .setCell("Ok", Boolean.toString(ok))
                            ;
                });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("AdmissionsCycleCheck.xlsx", stream.toByteArray());

        final AdmissionProcessTarget target = FenixFramework.getDomainObject("853027749632593");
        final JsonObject outcome = target.getOutcomeConfigJson();
        outcome.addProperty("cycleType", CycleType.SECOND_CYCLE.name());
        target.setOutcomeConfig(outcome.toString());
    }

    private CycleType cycleType(final JsonObject outcome) {
        final JsonElement cycleType = outcome.get("cycleType");
        return cycleType == null || cycleType.isJsonNull() ? null : CycleType.valueOf(cycleType.getAsString());
    }

}