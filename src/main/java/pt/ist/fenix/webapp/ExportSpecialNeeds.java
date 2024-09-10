package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.dynamicForms.DynamicForm;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

public class ExportSpecialNeeds extends CustomTask {
    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Disabilities");
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .forEach(ap -> {
                        ap.getAdmissionProcessTargetSet().stream()
                            .forEach(target -> {
                                target.getApplicationSet().stream()
                                    .filter(application -> application.getAdmitted())
                                    .filter(application -> application.isBoarding())
                                    .forEach(application -> {
                                        final AdmissionProcess admissionProcess = application.getAdmissionProcessTarget().getAdmissionProcess();

                                        final JsonObject formData = application.getDataObject().getAsJsonObject("formData");
                                        final JsonObject processForm = admissionProcess.getFormDataJson();
                                        process(application, formData, processForm, spreadsheet);

                                        final JsonObject outcomeBeforeFormData = application.getBeforeOutcomeFormDataJson();
                                        final JsonObject beforeOutcomeForm = admissionProcess.getBeforeOutcomeFormDataJson();
                                        process(application, outcomeBeforeFormData, beforeOutcomeForm, spreadsheet);

                                        final JsonObject outcomeAfterFormData = application.getAfterOutcomeFormDataJson();
                                        final JsonObject afterOutcomeForm = admissionProcess.getAfterOutcomeFormDataJson();
                                        process(application, outcomeAfterFormData, afterOutcomeForm, spreadsheet);
                                    });
                            });
                    }
                );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Alunos_necessidades_especiais_2021_2022.xls", baos.toByteArray());
    }

    private void process(final Application application, JsonObject formData, final JsonObject formTemplate, final Spreadsheet spreadsheet) {
        if (formData != null) {
            final DynamicForm dynamicForm = new DynamicForm(formTemplate)
                    .withData(formData);
            final DynamicForm.Field disabilities = dynamicForm.get("disabilities");
            final Boolean hasDisabilities = disabilities != null ? disabilities.value() : null;
            if (hasDisabilities != null && hasDisabilities) {
                final DynamicForm.Field disabilitiesDescription = dynamicForm.get("disabilitiesDescription");
                final String comments = disabilitiesDescription != null ? disabilitiesDescription.value() : null;
                report(application, comments, spreadsheet);
            }
        }
    }

    private void report(final Application application, final String disabilitiesDescription, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Processo", application.getAdmissionProcessTarget().getAdmissionProcess().getTitle().getContent());
        if (application.getDataObject().has("registration")) {
            final String registrationID = application.getDataObject().get("registration").getAsString();
            Registration registration = FenixFramework.getDomainObject(registrationID);
            row.setCell("Curso", registration.getDegreeCurricularPlanName());
            row.setCell("Nº Aluno", registration.getNumber());
        }
        row.setCell("Nome", application.getAccount().getIdentity().getPersonalInformation().getFullName());
        row.setCell("Telemóvel", application.getAccount().getMobile());
        row.setCell("Email", application.getAccount().getEmail());
        row.setCell("Comentários", disabilitiesDescription);
    }

    private String getValue(final String key, final JsonObject formData) {
        final Optional<String> value = formData.entrySet().stream()
                .map(entry -> entry.getValue())
                .filter(entry -> entry.isJsonObject())
                .flatMap(page -> page.getAsJsonObject().entrySet().stream())
                .flatMap(section -> section.getValue().getAsJsonObject().entrySet().stream())
                .filter(field -> key.equals(field.getKey()))
                .map(field -> field.getValue().getAsString())
                .findAny();
        return value.isPresent() ? value.get() : null;
    }
}
