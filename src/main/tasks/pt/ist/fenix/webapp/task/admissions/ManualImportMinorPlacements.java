package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsLog;
import org.fenixedu.admissions.domain.AdmissionsLogVisibility;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.ist.util.SheetUtils;
import org.fenixedu.admissions.ist.wizard.ComputeMinorResults;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ManualImportMinorPlacements extends WriteCustomTask implements SheetUtils {

    private static final Locale PT = new Locale("pt", "PT");
    private static final Locale EN = new Locale("en", "GB");

    @Override
    public void runTask() throws Exception {
        final String filename = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/minor-results_852907490541640ARv2.xlsx";
        final byte[] content = Files.readAllBytes(new File(filename).toPath());

        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("852907490541640");

        xlsxRowStream(content, "Results")
                .skip(1)
                .forEach(row -> {
                    final String applicationID = getCellValue(row.getCell(1));
                    final String grade = getCellValue(row.getCell(7));
                    final String minorPlacement = getCellValue(row.getCell(9));

                    final Application application = FenixFramework.getDomainObject(applicationID);
                    if (application.getAdmissionProcessTarget().getAdmissionProcess() == admissionProcess) {
                        application.setGrade(new BigDecimal(grade));
                        application.setAccepted(Boolean.TRUE);
                    }
                });

        if (!Utils.isMinor(admissionProcess) || Utils.isMinorDirectPlacement(admissionProcess)
                || !canPublishResults(admissionProcess)) {
            return;
        }

        admissionProcess.setStartOutcomePeriod(null);
        admissionProcess.setEndOutcomePeriod(null);
        admissionProcess.setHasAdmissionGranted(true);

        final AdmissionProcessTarget originalTarget = admissionProcess.getAdmissionProcessTargetSet().iterator().next();

        final Map<String, AdmissionProcessTarget> targetMap = new HashMap<>();
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        final JsonObject formData = admissionProcess.getFormDataJson();
        final JsonArray options = formData.getAsJsonArray("pages").get(0).getAsJsonObject()
                .getAsJsonArray("sections").get(0).getAsJsonObject()
                .getAsJsonArray("properties").get(0).getAsJsonObject()
                .getAsJsonArray("properties").get(0).getAsJsonObject()
                .getAsJsonArray("options");

        for (final JsonElement jsonElement : options) {
            final JsonObject option = jsonElement.getAsJsonObject();
            final String id = option.get("value").getAsString();
            final LocalizedString label = LocalizedString.fromJson(option.get("label"));
            final Integer slots = readSlots(option);

            final AdmissionProcessTarget target = new AdmissionProcessTarget(admissionProcess, label, slots);
            final JsonObject config = new JsonObject();
            config.add("actionName", ls("Inscrever", "Enroll").json());
            config.addProperty("degreeId", id);
            config.addProperty("executionYear", executionYear.getExternalId());
            target.setOutcomeConfig(config.toString());
            targetMap.put(id, target);
        }

        /*
        final Set<String> placements = spreadsheet.getNextSpreadsheet().getRows().stream()
                .map(row -> ((String) row.getCells().get(1)) + ":" + ((String) row.getCells().get(6)))
                .collect(Collectors.toSet());
         */
        final Set<String> placements = new HashSet<>();
        xlsxRowStream(content, "Results")
                .skip(1)
                .forEach(row -> {
                    final String applicationID = getCellValue(row.getCell(1));
                    final String grade = getCellValue(row.getCell(7));
                    final String minorPlacement = getCellValue(row.getCell(9));

                    taskLog("%s : %s : %s%n", applicationID, grade, minorPlacement);

                    if (minorPlacement != null && !"null".equals(minorPlacement)) {
                        final Map.Entry<String, AdmissionProcessTarget> entry = targetMap.entrySet().stream()
                                .filter(e -> e.getValue().getName().getContent().equals(minorPlacement))
                                .findAny().orElse(null);
                        if (entry == null) {
                            throw new Error("Unable to find attributed minor!");
                        }
                        placements.add(applicationID + ":" + entry.getKey());
                    }
                });

        originalTarget.getApplicationSet().stream()
                .filter(application -> application.getLockInstant() != null)
                .forEach(application -> {
                    final JsonArray minors = application.getDataObject().getAsJsonObject("formData")
                            .getAsJsonObject("0").getAsJsonObject("0").getAsJsonArray("minors");
                    for (final JsonElement jsonElement : minors) {
                        final JsonObject minorObject = jsonElement.getAsJsonObject().getAsJsonObject("minor");
                        final String minorId = minorObject.get("value").getAsString();
//                        final LocalizedString minorName = LocalizedString.fromJson(minorObject.get("label"));
                        final AdmissionProcessTarget selectedTarget = targetMap.get(minorId);

                        if (selectedTarget != null) {
                            final Application clone = copy(application, targetMap.get(minorId));
                            clone.setAccepted(Boolean.TRUE);
                            final boolean admitted = placements.contains(application.getExternalId() + ":" + minorId);
                            clone.setAdmitted(admitted);
                            if (admitted) {
                                taskLog("Admitted: %s%n", application.getExternalId());
                            }
                        }
                    }
                });

        originalTarget.getApplicationSet().stream()
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> application.getAccount().getApplicationSet().stream()
                        .filter(app -> app.getAdmissionProcessTarget().getAdmissionProcess() == application.getAdmissionProcessTarget().getAdmissionProcess())
                        .count() > 1l)
                .forEach(Application::delete);

        originalTarget.getApplicationSet().stream()
                .filter(application -> application.getLockInstant() != null)
                .forEach(application -> {
                    application.setAccepted(Boolean.FALSE);
                });

        new AdmissionsLog(AdmissionsLogVisibility.PROCESS_MANAGERS, admissionProcess,
                BundleUtil.getLocalizedString("resources.AdmissionsISTResources", "log.admissions.process.minor.publish.results"));

//        throw new Error("Abort TX");
    }

    private Application copy(final Application application, final AdmissionProcessTarget target) {
        final Application copy = new Application(application.getAccount(), target);
        copy.getLogSet().stream().forEach(AdmissionsLog::delete);
        copy.setCreatedInstant(application.getCreatedInstant());
        copy.setData(application.getData());
        copy.setGrade(application.getGrade());
        copy.setGradeConfig(application.getGradeConfig());
        copy.setLockInstant(application.getLockInstant());
        copy.setRevisionDeadline(application.getRevisionDeadline());
        application.getLogSet().forEach(log -> copy(log, copy));
        return copy;
    }

    private void copy(final AdmissionsLog log, final Application application) {
        final AdmissionsLog copy = new AdmissionsLog(log.getVisibility(), application, log.getDescription());
        copy.setWhen(log.getWhen());
        copy.setAccountEmail(log.getAccountEmail());
    }

    private Integer readSlots(final JsonObject option) {
        final JsonElement slots = option.get("slots");
        return slots == null || slots.isJsonNull() ? null : slots.getAsInt();
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }

    private boolean canPublishResults(final AdmissionProcess process) {
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> (application.getLockInstant() != null && application.getAccepted() == null)
                        || (application.getAccepted() != null && application.getAccepted() && application.getGrade() == null))
                .forEach(application -> {
                    taskLog("Application: %s has no grade%n", application.getExternalId());
                });

        return process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .noneMatch(application -> (application.getLockInstant() != null && application.getAccepted() == null)
                        || (application.getAccepted() != null && application.getAccepted() && application.getGrade() == null));
    }

}