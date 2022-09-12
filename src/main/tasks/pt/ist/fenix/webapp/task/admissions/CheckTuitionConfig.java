package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;

public class CheckTuitionConfig extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("TuitionMap");

        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> admissionProcess.getTitle().getContent().indexOf("2023") > 0)
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .forEach(target -> {
                    final JsonObject confi = target.getOutcomeConfigJson();
                    final JsonElement eventTemplate =  confi.get("eventTemplate");
                    if (eventTemplate != null && !eventTemplate.isJsonNull()) {
                        final JsonElement degreeE = confi.get("degree");
                        final Degree degree = degreeE == null || degreeE.isJsonNull() ? null : FenixFramework.getDomainObject(degreeE.getAsString());
                        final EventTemplate eventTemplate1 = FenixFramework.getDomainObject(eventTemplate.getAsString());

                        final Spreadsheet.Row row = spreadsheet.addRow();
                        row.setCell("Concurso", target.getAdmissionProcess().getTitle().getContent());
                        row.setCell("Degree", degree == null ? "" : degree.getSigla());
                        row.setCell("Plano", eventTemplate1.getCode());

                    }
                });


        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("tuitionMap.xlsx", stream.toByteArray());
    }
}