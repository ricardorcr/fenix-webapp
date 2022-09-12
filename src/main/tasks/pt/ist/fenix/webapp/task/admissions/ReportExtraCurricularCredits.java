package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.util.DynamicForm;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

public class ReportExtraCurricularCredits extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("AE");

        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("852907490541629");
        admissionProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .forEach(application -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Target", application.getAdmissionProcessTarget().getName().getContent());
                    row.setCell("User", application.getAccount().getIdentity().getUser().getUsername());
                    row.setCell("Locked", application.getLockInstant() == null ? ""
                            : application.getLockInstant().toString("yyyy-MM-dd"));
                    row.setCell("Validated", application.getAccepted() == null ? "No" : "Yes");
                    row.setCell("Grade", application.getGrade() == null ? "" : application.getGrade().toPlainString());

                    final JsonObject config = application.getGradeConfigJson();
                    final JsonArray components = config.getAsJsonArray("components");
                    final JsonObject gradeData = application.getGradeDataJson();
                    for (final JsonElement e : components) {
                        final JsonObject o = e.getAsJsonObject();
                        final String name = o.get("name").getAsString();
                        final LocalizedString description = LocalizedString.fromJson(o.get("description"));
                        final JsonElement valueElement = gradeData == null || gradeData.isJsonNull() ? null : gradeData.get(name);
                        final BigDecimal value = valueElement == null || valueElement.isJsonNull() ? null : valueElement.getAsBigDecimal();
                        row.setCell(description.getContent(), value == null ? "" : value.toPlainString());
                    }

                    final DynamicForm dynamicForm = new DynamicForm(application.getAdmissionProcessTarget().getAdmissionProcess().getFormDataJson());
                    dynamicForm.withData(application.getDataObject().getAsJsonObject("formData"));

                    final double sum = dynamicForm.all()
                            .filter(field -> "activityHours".equals(field.getName()))
                            .map(field -> (DynamicForm.Quantity) field)
                            .mapToDouble(field -> field.value().doubleValue())
                            .sum();

                    row.setCell("Duração Declarada", sum);
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("ae.xlsx", stream.toByteArray());
    }

}