package org.fenixedu.admissions.ist.wizard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsLog;
import org.fenixedu.admissions.domain.AdmissionsLogVisibility;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.stream.StreamUtils;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.Identity;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExportMinorResultsWizard extends ReadCustomTask implements RemoteReader {

    private static final long THRESHOLD = 1l;

    private final Map<String, JsonObject> levels = new HashMap<>();
    private final Map<Degree, Set<Degree>> incompatibilityMap = new HashMap<>();

    private static final Comparator<JsonObject> BY_LOCK_DATE = (j1, j2) -> {
        final String l1 = j1.get("locked").getAsString();
        final String l2 = j2.get("locked").getAsString();
        final int i = l2.compareTo(l1);
        return i == 0 ? j1.get("application").getAsString().compareTo(j2.get("application").getAsString()) : i;
    };

    private static final Comparator<JsonObject> BY_GRADE = (j1, j2) -> {
        final double d1 = gradeFor(j1);
        final double d2 = gradeFor(j2);
        return d1 == d2 ? BY_LOCK_DATE.compare(j1, j2) : Double.compare(d1, d2);
    };

    private static final Comparator<JsonObject> BY_ALLOWED_AND_GRADE = (j1, j2) -> {
        final int d1 = enrolledOrPlaced(j1);
        final int d2 = enrolledOrPlaced(j2);
        return d1 == d2 ? BY_GRADE.compare(j1, j2) : Double.compare(d1, d2);
    };

    public Spreadsheet computeAndExport(final AdmissionProcess process) {
        return computeAndExport(process, true);
    }

    public Spreadsheet computeAndExport(final AdmissionProcess process, final boolean log) {
        if (!Utils.isMinor(process) || !process.isProcessManager() || process.isOpenApplicationSubmissionPeriod()) {
            return null;
        }

        loadIncompatibilityMap();

        final Spreadsheet results = new Spreadsheet("Results");
        final Spreadsheet minorsSheet = results.addSpreadsheet("Minors");
        final Spreadsheet registrationSheet = minorsSheet.addSpreadsheet("Registrations");
        final Spreadsheet degreePlacementSheet = registrationSheet.addSpreadsheet("DegreePlacements");
        final Spreadsheet minorOptions = degreePlacementSheet.addSpreadsheet("Minor Options");

        final List<String> availableSlots = loadAvailableSlots(process);
        final List<String> placedSlots = new ArrayList<>();
        final List<String> chosen = new ArrayList<>();
        final Map<String, Spreadsheet.Row> minorRows = loadMinorRows(process, minorsSheet);

        final Map<String, Double> natureMap = loadNatureMap();
        final Map<Account, String> placementMap = new HashMap<>();
        final Map<Account, String> gradeMap = new HashMap<>();
        final Map<Account, Spreadsheet.Row> rowMap = new HashMap<>();

        final int[] order = new int[]{1};
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .map(Application::getAccount)
                .distinct()
                .map(account -> account.getApplicationSet().stream()
                        .filter(app -> app.getAdmissionProcessTarget().getAdmissionProcess() == process)
                        .findAny()
                        .orElseThrow(() -> new Error("unreachable code")))
                .filter(application -> application.getLockInstant() != null)
                .map(application -> toResult(natureMap, application))
                .sorted(BY_ALLOWED_AND_GRADE.reversed())
                .forEach(result -> {
                    final String id = result.get("application").getAsString();
                    final Application application = FenixFramework.getDomainObject(id);
                    final String username = JsonUtils.get(result, "username");

                    final double mfc = getDouble(result, "mfc");
                    final int o = mfc <= 0 ? -1 : order[0]++;
                    int incompatibleChoiceCount = 0;
                    final boolean hasPlacement = JsonUtils.getPrimitive(result, "hasPlacement").getAsBoolean();
                    final boolean hasPreviousPlacement = application.getAccount().getApplicationSet().stream()
                            .filter(app -> app != application && app.getAdmitted())
                            .anyMatch(app -> Utils.isMinor(app.getAdmissionProcessTarget().getAdmissionProcess()));


                    final Spreadsheet.Row row = results.addRow();
                    row.setCell("Order", o);
                    row.setCell("application", id);
                    row.setCell("account", result.get("account").getAsString());
                    row.setCell("username", username);
                    row.setCell("locked", JsonUtils.get(result, "locked"));
                    row.setCell("nature", getDouble(result, "nature"));
                    row.setCell("mfc", Long.toString(Math.round(mfc)));
                    row.setCell("grade", Long.toString(Math.round(getDouble(result, "grade"))));
                    row.setCell("hasPreviousPlacement", Boolean.toString(hasPreviousPlacement));
                    rowMap.put(application.getAccount(), row);

                    gradeMap.put(application.getAccount(), Long.toString(Math.round(getDouble(result, "grade"))));

                    if (o > 0 && !hasPreviousPlacement) {
                        final Set<Degree> possibleDegrees = readDegrees(result);

                        final JsonArray minors = application.getDataObject().getAsJsonObject("formData")
                                .getAsJsonObject("0").getAsJsonObject("0").getAsJsonArray("minors");
                        for (final JsonElement e : minors) {
                            final JsonObject minorObject = e.getAsJsonObject().getAsJsonObject("minor");
                            final String mID = minorObject.get("value").getAsString();
                            chosen.add(mID);
                        }
                        for (final JsonElement e : minors) {
                            final JsonObject minorObject = e.getAsJsonObject().getAsJsonObject("minor");
                            final String mID = minorObject.get("value").getAsString();
                            final Degree degree = FenixFramework.getDomainObject(mID);
                            if (isCompatible(application, degree, possibleDegrees, hasPlacement)) {
                                final String label = LocalizedString.fromJson(minorObject.get("label")).getContent();
                                if (availableSlots.remove(mID)) {
                                    row.setCell("placement", label);
                                    placedSlots.add(mID);
                                    placementMap.put(application.getAccount(), mID);
                                    break;
                                }
                            } else {
                                incompatibleChoiceCount++;
                            }
                        }
                        row.setCell("options", minors.size());
                    } else {
                    }

                    row.setCell("isEnrolled", JsonUtils.get(result, "isEnrolled"));
                    row.setCell("hasPlacement", Boolean.toString(hasPlacement));
                    row.setCell("incompatibleChoiceCount", incompatibleChoiceCount);
                    row.setCell("currentDegree", JsonUtils.get(result, "currentDegree"));
                    row.setCell("possibleTargetDegree", JsonUtils.get(result, "possibleTargetDegreeNames"));

                    final Account account = application.getAccount();
                    usersFor(account)
                            .map(User::getPerson)
                            .filter(Objects::nonNull)
                            .map(Person::getStudent)
                            .filter(Objects::nonNull)
                            .flatMap(s -> s.getRegistrationsSet().stream())
                            .forEach(registration -> {
                                final Spreadsheet.Row rrow = registrationSheet.addRow();
                                rrow.setCell("application", id);
                                rrow.setCell("account", result.get("account").getAsString());
                                rrow.setCell("username", username);
                                rrow.setCell("Degree", registration.getDegree().getSigla());
                                rrow.setCell("State", registration.getLastStateType().getName());
                            });

                    applicationsFor(account)
                            .filter(Application::getAdmitted)
                            .map(Application::getAdmissionProcessTarget)
                            .filter(target -> isDegreeProcess(target.getAdmissionProcess()))
                            .forEach(target -> {
                                final Spreadsheet.Row rrow = degreePlacementSheet.addRow();
                                rrow.setCell("application", id);
                                rrow.setCell("account", result.get("account").getAsString());
                                rrow.setCell("username", username);
                                rrow.setCell("Placement", target.getName().getContent());
                                rrow.setCell("AdmissionProcess", target.getAdmissionProcess().getTitle().getContent());
                            });

                    final JsonArray minors = application.getDataObject().getAsJsonObject("formData")
                            .getAsJsonObject("0").getAsJsonObject("0").getAsJsonArray("minors");
                    int i = 1;
                    for (final JsonElement e : minors) {
                        final JsonObject minorObject = e.getAsJsonObject().getAsJsonObject("minor");
                        final String mID = minorObject.get("value").getAsString();
                        final Degree minor = FenixFramework.getDomainObject(mID);
                        final Spreadsheet.Row mrow = minorOptions.addRow();

                        mrow.setCell("application", id);
                        mrow.setCell("account", result.get("account").getAsString());
                        mrow.setCell("username", username);
                        mrow.setCell("order", i++);
                        mrow.setCell("minor", minor.getPresentationName());
                    }
                });

        minorRows.forEach((k, v) -> v.setCell("Students",
                Long.toString(placedSlots.stream().filter(s -> s.equals(k)).count())));
        minorRows.forEach((k, v) -> v.setCell("Chosen By",
                Long.toString(chosen.stream().filter(s -> s.equals(k)).count())));

        if (log) {
            FenixFramework.atomic(() -> {
                new AdmissionsLog(AdmissionsLogVisibility.PROCESS_MANAGERS, process,
                        BundleUtil.getLocalizedString("resources.AdmissionsISTResources", "log.admissions.process.minor.export.results"));
            });
        }

        return results;
    }

    private Set<Degree> readDegrees(final JsonObject result) {
        final Set<Degree> degrees = new HashSet<>();
        final JsonArray possibleTargetDegreeNames = result.getAsJsonArray("possibleTargetDegree");
        for (final JsonElement e : possibleTargetDegreeNames) {
            degrees.add(FenixFramework.getDomainObject(e.getAsString()));
        }
        return degrees;
    }

    private boolean isCompatible(final Application application, final Degree minor, final Set<Degree> possibleDegrees,
            final boolean hasPlacement) {
        if (possibleDegrees.stream().anyMatch(degree -> !isCompatible(minor, degree))) {
            return false;
        }

        final Account account = application.getAccount();
        if (usersFor(account)
                .map(User::getPerson)
                .filter(Objects::nonNull)
                .map(Person::getStudent)
                .filter(Objects::nonNull)
                .flatMap(student -> student.getRegistrationsSet().stream())
                .filter(registration -> registration.isConcluded() || (!hasPlacement && registration.isActive()))
                .flatMap(this::toDegreeStream)
                .distinct()
                .anyMatch(degree -> !isCompatible(minor, degree))) {
            return false;
        }
        if (applicationsFor(account)
                .filter(Application::getAdmitted)
                .map(Application::getAdmissionProcessTarget)
                .filter(target -> isDegreeProcess(target.getAdmissionProcess()))
                .anyMatch(target -> !isCompatible(minor, degreeFor(target)))) {
            return false;
        }

        final boolean isEnrolled = usersFor(account)
                .map(User::getPerson)
                .filter(Objects::nonNull)
                .map(Person::getStudent)
                .filter(Objects::nonNull)
                .flatMap(student -> student.getRegistrationsSet().stream())
                .filter(Registration::isActive)
                .anyMatch(this::allowMinorForRegistrationComp);

        return isEnrolled || hasPlacement;
    }

    private boolean allowMinorForRegistrationComp(final Registration registration) {
        final Degree degree = registration.getDegree();
        if (degree.isSecondCycle() || degree.isFirstCycle()) {
            return true;
        }
        if (degree.isFirstCycle()) {
            final StudentCurricularPlan studentCurricularPlan = registration.getLastStudentCurricularPlan();
            return studentCurricularPlan.getCycleCurriculumGroups().stream()
                    .anyMatch(group -> group.getDegreeModule().getCycleType() == CycleType.SECOND_CYCLE
                            || (group.getDegreeModule().getCycleType() == CycleType.FIRST_CYCLE));
        }
        return false;
    }

    private boolean allowMinorForRegistration(final Registration registration) {
        final Degree degree = registration.getDegree();
        if (degree.isSecondCycle()) {
            return true;
        }
        if (degree.isFirstCycle()) {
            final StudentCurricularPlan studentCurricularPlan = registration.getLastStudentCurricularPlan();
            return studentCurricularPlan.getCycleCurriculumGroups().stream()
                    .anyMatch(group -> group.getDegreeModule().getCycleType() == CycleType.SECOND_CYCLE
                            || (group.getDegreeModule().getCycleType() == CycleType.FIRST_CYCLE) && group.isConcluded());
        }
        return false;
    }


    private Degree minorFor(final AdmissionProcessTarget target) {
        final JsonObject config = target.getOutcomeConfigJson();
        final JsonElement degree = config == null ? null : config.get("degreeId");
        return degree == null || degree.isJsonNull() ? null : FenixFramework.getDomainObject(degree.getAsString());
    }

    private Degree degreeFor(final AdmissionProcessTarget target) {
        final JsonObject config = target.getOutcomeConfigJson();
        return FenixFramework.getDomainObject(config.get("degree").getAsString());
    }

    private boolean isDegreeProcess(final AdmissionProcess admissionProcess) {
        final JsonObject config = admissionProcess.getOutcomeTypeJson();
        return config != null && "degree".equalsIgnoreCase(JsonUtils.getString(config, "name"));
    }

    private boolean isCompatible(final Degree minor, final Degree degree) {
        final Set<Degree> degrees = incompatibilityMap.get(minor);
        return degrees == null || !degrees.contains(degree);
    }

    private Stream<Degree> toDegreeStream(final Registration registration) {
        return Stream.concat(Stream.of(registration.getDegree()),
                registration.getStudentCurricularPlansSet().stream()
                        .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                        .map(group -> group.getDegreeModule().getDegree()));
    }

    private Stream<Application> applicationsFor(final Account account) {
        final Identity identity = account.getIdentity();
        return identity == null ? account.getApplicationSet().stream() : identity.getAccountSet().stream()
                .flatMap(a -> a.getApplicationSet().stream());
    }

    private Stream<User> usersFor(final Account account) {
        final Identity identity = account.getIdentity();
        return identity == null ? usersFor(account.getUser()) : Stream.concat(usersFor(identity.getUser()),
                identity.getAccountSet().stream().flatMap(a -> usersFor(a.getUser())));
    }

    private Stream<User> usersFor(final User user) {
        return user == null ? Stream.empty() : Stream.of(user);
    }

    private Map<String, Double> loadNatureMap() {
        final Map<String, Double> map = new HashMap<>();
        final String[] lines = lines("university_nature_values.csv");
        for (int i = 1; i < lines.length; i++) {
            final String line = lines[i];
            final int i1 = line.charAt(0) == '"' ? 1 : 0;
            final int i2 = i1 == 1 ? line.indexOf('"', i1) : line.indexOf(',');
            final int i3 = line.indexOf(',', i2);
            final int i4 = line.charAt(i3 + 1) == '"' ? (i3 + 2) : (i3 + 1);
            final int i5 = i3 + 2 == i4 ? line.length() - 1 : line.length();

            final String university = normalize(line.substring(i1, i2));
            final Double nature = Double.parseDouble(line.substring(i4, i5).replace(',', '.'));

            map.put(university, nature);
        }
        return map;
    }

    private void loadIncompatibilityMap() {
        final String[] lines = lines("MinorIncompatibilities.csv");
        for (int i = 1; i < lines.length; i++) {
            final String line = lines[i];
            final String[] parts = line.split(",");
            if (parts[2].length() > 1) {
                final Degree minorId = FenixFramework.getDomainObject(parts[0]);
                final Degree degreeId = FenixFramework.getDomainObject(parts[2]);
                incompatibilityMap.putIfAbsent(minorId, new HashSet<>());
                incompatibilityMap.get(minorId).add(degreeId);
            }
        }
    }

    private Map<String, Spreadsheet.Row> loadMinorRows(final AdmissionProcess admissionProcess, final Spreadsheet courses) {
        final Map<String, Spreadsheet.Row> result = new HashMap<>();

        final JsonObject formData = admissionProcess.getFormDataJson();
        final JsonArray options = formData.getAsJsonArray("pages").get(0).getAsJsonObject()
                .getAsJsonArray("sections").get(0).getAsJsonObject()
                .getAsJsonArray("properties").get(0).getAsJsonObject()
                .getAsJsonArray("properties").get(0).getAsJsonObject()
                .getAsJsonArray("options");
        for (final JsonElement e : options) {
            final JsonObject option = e.getAsJsonObject();
            final String id = option.get("value").getAsString();
            final Integer slots = readSlots(option);
            final String label = LocalizedString.fromJson(option.get("label")).getContent();

            final Spreadsheet.Row row = courses.addRow();
            row.setCell("Minor", label);
            row.setCell("Slots", slots == null ? "" : slots.toString());
            result.put(id, row);
        }

        return result;
    }

    private List<String> loadAvailableSlots(final AdmissionProcess admissionProcess) {
        final List<String> result = new ArrayList<>();

        final long applicationCount = admissionProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .count();

        final JsonObject formData = admissionProcess.getFormDataJson();
        final JsonArray options = formData.getAsJsonArray("pages").get(0).getAsJsonObject()
                .getAsJsonArray("sections").get(0).getAsJsonObject()
                .getAsJsonArray("properties").get(0).getAsJsonObject()
                .getAsJsonArray("properties").get(0).getAsJsonObject()
                .getAsJsonArray("options");
        for (final JsonElement e : options) {
            final JsonObject option = e.getAsJsonObject();
            final String id = option.get("value").getAsString();
            final Integer slots = readSlots(option);
            final long slotsToCreate = slots == null ? applicationCount : slots;
            for (int i = 0; i < slotsToCreate; i++) {
                result.add(id);
            }
        }

        return result;
    }

    private Integer readSlots(final JsonObject option) {
        final JsonElement slots = option.get("slots");
        return slots == null || slots.isJsonNull() ? null : slots.getAsInt();
    }

    private JsonObject toResult(final Map<String, Double> natureMap, final Application application) {
        final Account account = application.getAccount();
        final Identity identity = account.getIdentity();
        final User user = identity == null ? null : identity.getUser();
        final Student student = user == null ? null : user.getPerson().getStudent();

        double mfc = -1d;
        boolean isISTStudent = false;
        if (student != null) {
            mfc = student.getRegistrationsSet().stream()
                    .filter(this::hasConcludedFirstCycle)
                    .map(registration -> finalGradeForCycle(registration, CycleType.FIRST_CYCLE))
                    .mapToDouble(grade -> grade.getNumericValue().doubleValue() * 10)
                    .max().orElse(-1d);
            if (mfc < 0d) {
                mfc = student.getRegistrationsSet().stream()
                        .filter(this::isSecondCycle)
                        .filter(Registration::isActive)
                        .flatMap(registration -> registration.getPrecedentDegreesInformationsSet().stream())
                        .mapToDouble(info -> new BigDecimal(info.getConclusionGrade()).doubleValue() * 10)
                        .max().orElse(-1d);
            }
            if (mfc < 0d) {
                mfc = student.getRegistrationsSet().stream()
                        .filter(this::isFirstCycle)
                        .filter(Registration::isActive)
                        .map(registration -> finalGradeForCycle(registration, CycleType.FIRST_CYCLE))
                        .mapToDouble(grade -> grade.getNumericValue().doubleValue() * 10)
                        .max().orElse(-1d);
            }
        }
        double appNature = 1d;
        if (mfc < 0) {
            final JsonObject appGrade = applicationStreamFor(application.getAccount())
                    .filter(app -> app != application)
                    .filter(app -> app.getLockInstant() != null)
                    .filter(Application::getAdmitted)
                    .map(app -> gradeDataFor(natureMap, app))
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(this::calc))
                    .orElse(null);
            if (appGrade != null) {
                appNature = appGrade.get("b").getAsDouble();
                mfc = appGrade.get("c").getAsDouble();
            }
        } else {
            isISTStudent = true;
        }

        final boolean isEnrolled = usersFor(account)
                .map(User::getPerson)
                .filter(Objects::nonNull)
                .map(Person::getStudent)
                .filter(Objects::nonNull)
                .flatMap(s -> s.getRegistrationsSet().stream())
                .filter(Registration::isActive)
                .anyMatch(this::allowMinorForRegistration);

        final boolean hasPlacement = applicationsFor(account)
                .filter(Application::getAdmitted)
                .map(Application::getAdmissionProcessTarget)
                .anyMatch(target -> isDegreeProcess(target.getAdmissionProcess()));

        final String currentDegree = usersFor(account)
                .map(User::getPerson)
                .filter(Objects::nonNull)
                .map(Person::getStudent)
                .filter(Objects::nonNull)
                .flatMap(s -> s.getRegistrationsSet().stream())
                .filter(Registration::isActive)
                .map(registration -> registration.getDegree().getSigla())
                .distinct()
                .collect(Collectors.joining(", "));

        final Stream<Degree> possibleStream = hasPlacement ? applicationsFor(account)
                .filter(Application::getAdmitted)
                .map(Application::getAdmissionProcessTarget)
                .filter(target -> isDegreeProcess(target.getAdmissionProcess()))
                .map(this::degreeFor)
                : Stream.concat(Stream.concat(
                        usersFor(account)
                                .map(User::getPerson)
                                .filter(Objects::nonNull)
                                .map(Person::getStudent)
                                .filter(Objects::nonNull)
                                .flatMap(s -> s.getRegistrationsSet().stream())
                                .filter(Registration::isActive)
                                .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                                .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                                .filter(group -> group.getCycleType() == CycleType.SECOND_CYCLE)
                                .map(group -> group.getDegreeModule().getDegree()),
                        usersFor(account)
                                .map(User::getPerson)
                                .filter(Objects::nonNull)
                                .map(Person::getStudent)
                                .filter(Objects::nonNull)
                                .flatMap(s -> s.getRegistrationsSet().stream())
                                .filter(Registration::isActive)
                                .map(Registration::getDegree)
                                .filter(Degree::isSecondCycle)),
                usersFor(account)
                        .map(User::getPerson)
                        .filter(Objects::nonNull)
                        .map(Person::getStudent)
                        .filter(Objects::nonNull)
                        .flatMap(s -> s.getRegistrationsSet().stream())
                        .filter(Registration::isActive)
                        .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                        .flatMap(scp -> scp.getDegreeCurricularPlan().getDestinationAffinities(CycleType.FIRST_CYCLE).stream())
                        .map(group -> group.getDegree()));

        final StringBuilder possibleTargetDegreeNames = new StringBuilder();
        final JsonArray possibleTargetDegree = possibleStream
                .distinct()
                .filter(degree -> degree.getDegreeCurricularPlansSet().stream()
                        .flatMap(dcp -> dcp.getExecutionDegreesSet().stream())
                        .anyMatch(ed -> ed.getExecutionYear().isCurrent()))
                .peek(degree -> {
                    if (possibleTargetDegreeNames.length() > 0) {
                        possibleTargetDegreeNames.append(", ");
                    }
                    possibleTargetDegreeNames.append(degree.getSigla());
                })
                .map(degree -> new JsonPrimitive(degree.getExternalId()))
                .collect(StreamUtils.toJsonArray());

        final double nature = isISTStudent ? 5 : appNature;
        final double mfcValue = mfc;
        return JsonUtils.toJson(result -> {
            result.addProperty("application", application.getExternalId());
            result.addProperty("account", account.getEmail());
            result.addProperty("username", user == null ? "" : user.getUsername());
            result.addProperty("locked", application.getLockInstant().toString("yyyy-MM-dd HH:mm:ss"));
            result.addProperty("nature", nature);
            result.addProperty("mfc", mfcValue);
            result.addProperty("grade", mfcValue <= 0 ? 0d : (20 * nature) + (0.5 * mfcValue));
            result.addProperty("isEnrolled", isEnrolled);
            result.addProperty("hasPlacement", hasPlacement);
            result.addProperty("enrolledOrPlaced", isEnrolled || hasPlacement);
            result.addProperty("currentDegree", currentDegree);
            result.add("possibleTargetDegree", possibleTargetDegree);
            result.addProperty("possibleTargetDegreeNames", possibleTargetDegreeNames.toString());

        });
    }

    private Double calc(final JsonObject appGrade) {
        final double nature = appGrade.get("b").getAsDouble();
        final double mfc = appGrade.get("c").getAsDouble();
        return mfc <= 0 ? 0d : (20 * nature) + (0.5 * mfc);
    }

    private Stream<Application> applicationStreamFor(final Account account) {
        final Identity identity = account.getIdentity();
        return identity == null ? account.getApplicationSet().stream() : identity.getAccountSet().stream()
                .flatMap(a -> a.getApplicationSet().stream());
    }

    private JsonObject gradeDataFor(final Map<String, Double> natureMap, final Application application) {
        final JsonObject data = application.getDataObject();
        final JsonObject officialGradeData = data.getAsJsonObject("gradeData");
        if (officialGradeData != null && !officialGradeData.isJsonNull()) {
            return officialGradeData;
        }
        final JsonObject formData = data.getAsJsonObject("formData");
        final String[] index = qualificationsIndex(application.getAdmissionProcessTarget().getAdmissionProcess());
        if (index != null) {
            final JsonObject page = formData.getAsJsonObject(index[0]);
            if (page != null && !page.isJsonNull()) {
                final JsonObject section = page.getAsJsonObject(index[1]);
                if (section != null && !section.isJsonNull()) {
                    final JsonArray qualifications = section.getAsJsonArray("qualifications");
                    JsonObject bestGrade = null;
                    if (qualifications != null && !qualifications.isJsonNull()) {
                        for (final JsonElement qe : qualifications) {
                            final JsonObject qualification = qe.getAsJsonObject();
                            final JsonObject gradeData = gradeFor(natureMap, qualification, application);
                            if (gradeData != null && (bestGrade == null || application.calculateGrade(gradeData).doubleValue() >= application.calculateGrade(bestGrade).doubleValue())) {
                                bestGrade = gradeData;
                            }
                        }
                    }
                    return bestGrade;
                }
            }
        }
        return null;
    }

    private String[] qualificationsIndex(final AdmissionProcess admissionProcess) {
        final JsonObject formData = admissionProcess.getFormDataJson();
        int p = 0;
        for (final JsonElement page : formData.getAsJsonArray("pages")) {
            int s = 0;
            for (final JsonElement section : page.getAsJsonObject().getAsJsonArray("sections")) {
                for (final JsonElement propertyE : section.getAsJsonObject().getAsJsonArray("properties")) {
                    final JsonObject property = propertyE.getAsJsonObject();
                    if ("Array".equalsIgnoreCase(property.get("type").getAsString())
                            && "qualifications".equalsIgnoreCase(property.get("field").getAsString())) {
                        updateLevels(property);
                        return new String[]{Integer.toString(p), Integer.toString(s)};
                    }
                }
                s++;
            }
            p++;
        }
        return null;
    }

    private void updateLevels(final JsonObject qualifications) {
        for (final JsonElement element : qualifications.getAsJsonArray("properties")) {
            final JsonObject property = element.getAsJsonObject();
            if ("Select".equalsIgnoreCase(property.get("type").getAsString())
                    && "qualificationLevel".equalsIgnoreCase(property.get("field").getAsString())) {
                for (final JsonElement options : property.getAsJsonArray("options")) {
                    final JsonObject option = options.getAsJsonObject();
                    final String key = option.get("value").getAsString();
                    if (levels.containsKey(key)) {
                        if (!option.toString().equalsIgnoreCase(levels.get(key).toString())) {
                            throw new Error("Levels don't match: " + option.toString() + " != " + levels.get(key).toString());
                        }
                    } else {
                        levels.put(key, option);
                    }
                }
            }
        }
    }

    private boolean hasConcludedFirstCycle(final Registration registration) {
        return registration.getStudentCurricularPlansSet().stream()
                .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                .filter(group -> group.getCycleType() == CycleType.FIRST_CYCLE)
                .anyMatch(CurriculumGroup::isConcluded);
    }

    private Grade finalGradeForCycle(final Registration registration, final CycleType cycleType) {
        final ICurriculum curriculum = registration.getCurriculum(cycleType);
        return curriculum.getRawGrade();
    }

    private Stream<CycleCurriculumGroup> cycleGroups(final Student student) {
        return student.getRegistrationsSet().stream()
                .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                .filter(group -> group.getCycleType() == CycleType.FIRST_CYCLE);
    }

    private boolean isFirstCycle(final Registration registration) {
        final Degree degree = registration.getDegree();
        return !degree.isEmpty() && degree.getCycleTypes().stream()
                .anyMatch(cycleType -> cycleType == CycleType.FIRST_CYCLE);
    }

    private boolean isSecondCycle(final Registration registration) {
        final Degree degree = registration.getDegree();
        return !degree.isEmpty() && registration.getStudentCurricularPlansSet().stream()
                .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                .anyMatch(group -> group.getCycleType() == CycleType.SECOND_CYCLE)
                && registration.getStudentCurricularPlansSet().stream()
                .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                .noneMatch(group -> group.getCycleType() == CycleType.FIRST_CYCLE);
    }

    public static Double getDouble(final JsonObject json, final String property) {
        final JsonElement element = json.get(property);
        return element == null || element.isJsonNull() ? null : element.getAsDouble();
    }

    private static double gradeFor(final JsonObject json) {
        final JsonElement entryGrade = json.get("grade");
        return entryGrade == null || entryGrade.isJsonNull() ? 0d : entryGrade.getAsDouble();
    }

    private static int enrolledOrPlaced(final JsonObject json) {
        final JsonElement enrolledOrPlaced = json.get("enrolledOrPlaced");
        return enrolledOrPlaced == null || enrolledOrPlaced.isJsonNull() || !enrolledOrPlaced.getAsBoolean() ? 0 : 1;
    }

    private JsonObject gradeFor(final Map<String, Double> natureMap, final JsonObject qualification, final Application application) {
        final JsonElement qualificationLevel = qualification.get("qualificationLevel");
        if (!levels.isEmpty() && qualificationLevel != null && !qualificationLevel.isJsonNull() && (
                match(qualificationLevel.toString(), levels.get("qualificationLevel6").toString())
                        || match(qualificationLevel.toString(), levels.get("qualificationLevel7").toString())
                        || match(qualificationLevel.toString(), levels.get("qualificationLevel8").toString()))) {
            final JsonElement qualificationInstitution = qualification.get("qualificationInstitution");
            if (qualificationInstitution != null && !qualificationInstitution.isJsonNull()) {
                final String institution = structuredInstitution(application.getAdmissionProcessTarget().getAdmissionProcess())
                        ? LocalizedString.fromJson(qualificationInstitution.getAsJsonObject().get("label")).getContent()
                        : qualificationInstitution.getAsString();
                final JsonObject gradeData = new JsonObject();

                Double nature = natureFor(natureMap, institution);
                if (nature == null) {
                    nature = new Double(1);
                }
                int mfc = 0;
                final JsonElement qualificationGrade = qualification.get("qualificationGrade");
                if (qualificationGrade != null && !qualificationGrade.isJsonNull()) {
                    final String qualificationGradeValue = qualificationGrade.getAsString();
                    mfc = toMFC(qualificationGradeValue);
                }

                gradeData.addProperty("a", 0); // afinidade
                gradeData.addProperty("b", nature.doubleValue()); // natureza
                gradeData.addProperty("c", mfc); // mfc
                gradeData.addProperty("d", 0); // bonus
                return gradeData;
            }
        }
        return null;
    }

    private boolean match(final String s1, final String s2) {
        return !s1.isEmpty() && !s2.isEmpty() &&
                StringNormalizer.normalizeAndRemoveAccents(s1).equalsIgnoreCase(StringNormalizer.normalizeAndRemoveAccents(s2));
    }

    private boolean structuredInstitution(final AdmissionProcess admissionProcess) {
        final JsonObject formData = admissionProcess.getFormDataJson();
        for (final JsonElement page : formData.getAsJsonArray("pages")) {
            for (final JsonElement section : page.getAsJsonObject().getAsJsonArray("sections")) {
                for (final JsonElement propertyE : section.getAsJsonObject().getAsJsonArray("properties")) {
                    final JsonObject property = propertyE.getAsJsonObject();
                    if ("Array".equalsIgnoreCase(property.get("type").getAsString())
                            && "qualifications".equalsIgnoreCase(property.get("field").getAsString())) {
                        for (final JsonElement qpropertyE : property.getAsJsonArray("properties")) {
                            final JsonObject qproperty = qpropertyE.getAsJsonObject();
                            if ("qualificationInstitution".equalsIgnoreCase(qproperty.get("field").getAsString())) {
                                if ("Text".equalsIgnoreCase(qproperty.get("type").getAsString())) {
                                    return false;
                                } else if ("AsyncSelect".equalsIgnoreCase(qproperty.get("type").getAsString())) {
                                    return true;
                                } else {
                                    throw new Error("Unexpected institution type: " + admissionProcess.getExternalId());
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private Double natureFor(final Map<String, Double> natureMap, final String institution) {
        final String searchKey = fix(normalize(institution));
        Double result = natureMap.get(searchKey);
        if (result == null) {
            final double d = natureMap.entrySet().stream()
                    .filter(e -> searchKey.contains(e.getKey()) || e.getKey().contains(searchKey))
                    .mapToDouble(Map.Entry::getValue)
                    .max().orElse(-1d);
            if (d > 0d) {
                result = d;
            }
        }
        return result;
    }

    private static String normalize(final String input) {
        return StringNormalizer.normalizeAndRemoveAccents(input)
                .toLowerCase()
                .replace("-", " ")
                .trim()
                .replaceAll(" +", " ");
    }

    private static String replace(final String input, final String x, final String y) {
        return input.replace(normalize(x), normalize(y));
    }

    private String fix(final String s) {
        final String result = replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(
                replace(replace(replace(replace(replace(replace(normalize(s),
                        "Outra opção: ", ""),
                        "Instituto Superior Técnico (TagusPark)", "Universidade de Lisboa"),
                        "Instituto Superior Técnico", "Universidade de Lisboa"),
                        "Universidade Técnica de Lisboa", "Universidade de Lisboa"),
                        "Faculdade de Motricidade Humana", "Universidade de Lisboa"),
                        "Instituto de Geografia e Ordenamento do Território", "Universidade de Lisboa"),
                "FCUL", "Universidade de Lisboa"),
                "ISEG ", "Universidade de Lisboa"),
                "(ISEG)", "Universidade de Lisboa"),
                //"ISCTE Instituto Universitário de Lisboa", "ISCTE Instituto Universitário de Lisboa"),
                "ISCTE IUL", "ISCTE Instituto Universitário de Lisboa"),
                "FCT UNL", "Universidade Nova de Lisboa"),
                "Nova School of Business and Economics", "Universidade Nova de Lisboa"),
                "NOVA School of Science and Technology", "Universidade Nova de Lisboa"),
                " da Guarda", " de Guarda"),
                "universiadade", "Universidade"),
                "institudo", "Instituto"),
                "Instituo ", "Instituto");
        if (result.startsWith("iscte ")) {
            return normalize("ISCTE Instituto Universitário de Lisboa");
        }
        if (result.equals("university of coimbra")) {
            return normalize("Universidade de Coimbra");
        }
        if (result.equals("universidade e da beira interior")) {
            return normalize("Universidade da Beira Interior");
        }
        if (result.startsWith("instituto superior de agronomia")) {
            return normalize("Universidade de Lisboa");
        }
        if (result.equals("faculdade ciencias e tecnologias nova de lisboa")) {
            return normalize("Universidade Nova de Lisboa");
        }
        if (result.equals("universidade da mafeira")) {
            return normalize("Universidade da Madeira");
        }
        if (result.equals("nova sst")) {
            return normalize("Universidade Nova de Lisboa");
        }
        if (result.equals("instituto superior de economia e gestao")) {
            return normalize("Universidade de Lisboa");
        }
        if (result.startsWith("escola") && result.endsWith("henrique") && result.indexOf("nautica") > 0) {
            return normalize("Escola Náutica Infante Dom Henrique");
        }
        return result;
    }

    private int toMFC(String value) {
        value = value
                .replace("(0,00-20,00)", "(0-20)")
                .replace("( ", "(")
                .replace(" - ", "-")
                .replace("- ", "-")
                .replace("(0-20)", "")
                .replace("/20", "")
                .replace(" ", "")
                .replace(',', '.')
                .replace("valores", "")
                .replace("(diplomapedido.notafinalaconfirmar)", "")
                .trim();
        final int i = value.indexOf(';');
        if (i > 0) {
            value = value.substring(0, i);
        }
        if (value.startsWith("B+")) {
            return 178;
        }
        if (value.length() == 2 && StringUtils.isNumeric(value)) {
            return Integer.parseInt(value) * 10;
        }
        if (value.length() == 4 || value.length() == 5 || value.length() == 6) {
            try {
                return (int) Math.round(Double.parseDouble(value) * 10);
            } catch (final NumberFormatException ex) {
            }
        }
        if (value.endsWith("(0-100)")) {
            final String tvalue = value.replace("(0-100)", "");
            try {
                return (int) Math.round(Double.parseDouble(tvalue) * 2);
            } catch (final NumberFormatException ex) {
            }
        }
        if (value.endsWith("(0-10)")) {
            final String tvalue = value.replace("(0-10)", "");
            try {
                return (int) Math.round(Double.parseDouble(tvalue) * 20);
            } catch (final NumberFormatException ex) {
            }
        }
        if (value.endsWith("(0-5)")) {
            final String tvalue = value.replace("(0-5)", "");
            try {
                return (int) Math.round(Double.parseDouble(tvalue) * 40);
            } catch (final NumberFormatException ex) {
            }
        }

        return -1;
    }

    @Override
    public void runTask() throws Exception {
        final Map<String, Double> natureMap = loadNatureMap();
        final Application application = FenixFramework.getDomainObject("852890310676841");
        double appNature = 1d;
//        if (mfc < 0) {
            final JsonObject appGrade = applicationStreamFor(application.getAccount())
                    .filter(app -> app != application)
                    .filter(app -> app.getLockInstant() != null)
                    .filter(Application::getAdmitted)
                    .peek(app -> taskLog("app: %s : %s : %s : %s%n", app.getExternalId(),
                            app.getAdmissionProcessTarget().getName().getContent(),
                            app.getAdmissionProcessTarget().getAdmissionProcess().getTitle().getContent(),
                            gradeDataFor(natureMap, app)))
                    .map(app -> gradeDataFor(natureMap, app))
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(this::calc))
                    .orElse(null);
            if (appGrade != null) {
                appNature = appGrade.get("b").getAsDouble();
//                mfc = appGrade.get("c").getAsDouble();
            }
//        } else {
//            isISTStudent = true;
//        }

        taskLog("appNature = %s%n", appNature);

    }
}