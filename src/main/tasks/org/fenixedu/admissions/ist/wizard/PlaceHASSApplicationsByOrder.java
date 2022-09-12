package org.fenixedu.admissions.ist.wizard;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsLog;
import org.fenixedu.admissions.domain.AdmissionsLogVisibility;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.domain.Application_Base;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.Identity;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlaceHASSApplicationsByOrder extends WriteCustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("852907490541651");
        final Spreadsheet spreadsheet = new PlaceHASSApplicationsByOrder().place(process);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("placements.xlsx", stream.toByteArray());
    }

    private final Map<CurricularCourse, Set<Degree>> incompatibilityMap = new HashMap<>();

    @Atomic
    public Spreadsheet place(final AdmissionProcess process) {
        if (!Utils.isHACS(process) || !Utils.isHACSWithFirstComeFirstServe(process) || !process.isProcessManager()
                || process.getResultsOfficial()) {
            return null;
        }

        final Spreadsheet spreadsheet = new Spreadsheet("Placements");
        final Spreadsheet courses = spreadsheet.addSpreadsheet("Courses");

        loadIncompatibilityMap();

        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> !application.getAdmitted())
                .sorted(Comparator.comparing(Application_Base::getLockInstant))
                .forEach(application -> {
                    final Account account = application.getAccount();
                    final CurricularCourse curricularCourse = curricularCourseFor(application);

                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("When", application.getLockInstant().toString("yyyy-MM-dd HH:mm"));
                    row.setCell("Who", application.getAccount().getIdentity().getUser().getUsername());
                    row.setCell("CurricularCourse", curricularCourse.getName());

                    if (!hasCompletedCourse(application, curricularCourse)) {
                        row.setCell("Completed Course", Boolean.FALSE.toString());
                        final Set<Degree> possibleDegrees = possibleDegrees(application);
                        final boolean hasPlacement = applicationsFor(account)
                                .filter(Application::getAdmitted)
                                .map(Application::getAdmissionProcessTarget)
                                .anyMatch(target -> isDegreeProcess(target.getAdmissionProcess()));
                        if (isCompatible(application, curricularCourse, possibleDegrees, hasPlacement)) {
                            row.setCell("Is Compatible", Boolean.TRUE.toString());
                            final AdmissionProcessTarget target = application.getAdmissionProcessTarget();
                            final int availableSlots = target.getSlots() - (int) target.getApplicationSet().stream()
                                    .filter(Application::getAdmitted)
                                    .count();
                            final double expectedTransitionedHASSCredits = calculateExpectedTransitionnedHASSCredits(application);
                            final double currentHACSEnrolmentsPendingEvaluation = calculateCurrentHACSEnrolmentsPendingEvaluation(application);
                            final double totalECTS = applicationsFor(account)
                                    .filter(app -> Utils.isHACS(app.getAdmissionProcessTarget().getAdmissionProcess()))
                                    .filter(Application::getAdmitted)
                                    .mapToDouble(app -> curricularCourseFor(app).getEctsCredits())
                                    .sum();
                            row.setCell("ECTS Colocações Previas", Double.toString(totalECTS));
                            row.setCell("ECTS HACS Transitados/Concluídos", Double.toString(expectedTransitionedHASSCredits));
                            row.setCell("ECTS HACS Pendentes Avaliação", Double.toString(currentHACSEnrolmentsPendingEvaluation));
                            if (totalECTS < 6d
                                    && totalECTS + expectedTransitionedHASSCredits + currentHACSEnrolmentsPendingEvaluation < 9d
                                    && availableSlots > 0) {
                                application.setAccepted(Boolean.TRUE);
                                application.setAdmitted(true);
                                row.setCell("Placed", Boolean.TRUE.toString());
                            } else {
                                row.setCell("Placed", Boolean.FALSE.toString());
                            }
                        } else {
                            row.setCell("Is Compatible", Boolean.FALSE.toString());
                        }
                    } else {
                        row.setCell("Completed Course", Boolean.TRUE.toString());
                    }
                });

        process.getAdmissionProcessTargetSet().stream()
                .forEach(target -> {
                    final String label = target.getName().getContent();
                    final int i = label.indexOf(" - ");
                    final Integer slots = target.getSlots();

                    final Spreadsheet.Row row = courses.addRow();
                    row.setCell("Institution", label.substring(0, i));
                    row.setCell("CurricularCourse Name", label.substring(i + 3));
                    row.setCell("Slots", slots == null ? "" : slots.toString());
                    row.setCell("Placements", (int) target.getApplicationSet().stream().filter(Application::getAdmitted).count());
                    row.setCell("Applications", target.getApplicationSet().size());
                });

        new AdmissionsLog(AdmissionsLogVisibility.PROCESS_MANAGERS, process,
                BundleUtil.getLocalizedString("resources.AdmissionsISTResources", "log.admissions.process.hass.place.applications.by.order"));

        return spreadsheet;
    }

    private double calculateExpectedTransitionnedHASSCredits(final Application application) {
        final Identity identity = application.getAccount().getIdentity();
        final User user = identity == null ? null : identity.getUser();
        final Student student = user == null ? null : user.getPerson().getStudent();
        double result = 0d;
        if (student != null) {
            boolean[] hasGroup = new boolean[] { false };
            result = student.getRegistrationsSet().stream()
                    .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                    .flatMap(scp -> scp.getRoot().getAllCurriculumGroups().stream())
                    .filter(PlaceHASSApplicationsByOrder::isHASS)
                    .peek(cg -> hasGroup[0] = true)
                    .mapToDouble(CurriculumGroup::getAprovedEctsCredits)
                    .sum();
            if (!hasGroup[0]) {
                // nothing to do.
            }
        }
        return result;
    }

    private static boolean isHASS(final CurriculumGroup curriculumGroup) {
        final CourseGroup courseGroup = curriculumGroup.getDegreeModule();
        return courseGroup != null && (courseGroup.getName().contains("Humanidades")
                || courseGroup.getName().contains("Humanities"));
    }

    private CurricularCourse curricularCourseFor(final Application application) {
        final AdmissionProcessTarget target = application.getAdmissionProcessTarget();
        final JsonObject config = target.getOutcomeConfigJson();
        return FenixFramework.getDomainObject(config.get("curricularCourse").getAsString());
    }

    private void loadIncompatibilityMap() {
        final String[] lines = lines("hacsincompatibilities.csv");
        for (int i = 1; i < lines.length; i++) {
            final String line = lines[i];
            final String[] parts = line.split(",");
            if (parts[2].length() > 1) {
                final CurricularCourse curricularCourseId = FenixFramework.getDomainObject(parts[0]);
                final Degree degreeId = Bennu.getInstance().getDegreesSet().stream()
                        .filter(degree -> parts[3].equals(degree.getSigla()))
                        .findAny().orElse(null);
                incompatibilityMap.computeIfAbsent(curricularCourseId, ccid -> new HashSet<>()).add(degreeId);
            }
        }
    }

    private boolean hasCompletedCourse(final Application application, final CurricularCourse curricularCourse) {
        final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
        final Identity identity = application.getAccount().getIdentity();
        final User user = identity == null ? null : identity.getUser();
        final Student student = user == null ? null : user.getPerson().getStudent();
        return student != null && competenceCourse != null && student.getRegistrationsSet().stream()
                .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                .flatMap(scp -> scp.getRoot().getCurriculumLineStream())
                .filter(CurriculumLine::isApproved)
                .flatMap(this::toCompetenceCourses)
                .anyMatch(cc -> cc == competenceCourse);
    }

    private Stream<?> toCompetenceCourses(final CurriculumLine line) {
        final CompetenceCourse competenceCourse;
        if (line.isEnrolment()) {
            final Enrolment enrolment = (Enrolment) line;
            competenceCourse = enrolment.getCurricularCourse().getCompetenceCourse();
        } else {
            final Dismissal dismissal = (Dismissal) line;
            final DegreeModule degreeModule = dismissal.getDegreeModule();
            competenceCourse = degreeModule == null ? null
                    : ((CurricularCourse) degreeModule).getCompetenceCourse();
        }
        return competenceCourse == null ? Stream.empty() : Stream.of(competenceCourse);
    }

    private boolean isCompatible(final CurricularCourse curricularCourse, final Degree degree) {
        final Set<Degree> degrees = incompatibilityMap.get(curricularCourse);
        return degrees == null || !degrees.contains(degree);
    }

    private boolean isCompatible(final Application application, final CurricularCourse curricularCourse,
                                 final Set<Degree> possibleDegrees, final boolean hasPlacement) {
        if (possibleDegrees.stream().anyMatch(degree -> !isCompatible(curricularCourse, degree))) {
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
                .anyMatch(degree -> !isCompatible(curricularCourse, degree))) {
            return false;
        }
        if (applicationsFor(account)
                .filter(Application::getAdmitted)
                .map(Application::getAdmissionProcessTarget)
                .filter(target -> isDegreeProcess(target.getAdmissionProcess()))
                .anyMatch(target -> !isCompatible(curricularCourse, degreeFor(target)))) {
            return false;
        }

        final boolean isEnrolled = usersFor(account)
                .map(User::getPerson)
                .filter(Objects::nonNull)
                .map(Person::getStudent)
                .filter(Objects::nonNull)
                .flatMap(student -> student.getRegistrationsSet().stream())
                .filter(Registration::isActive)
                .anyMatch(registration -> registration.getDegreeType().isFirstCycle());

        return isEnrolled || hasPlacement;
    }

    private boolean isDegreeProcess(final AdmissionProcess admissionProcess) {
        final JsonObject config = admissionProcess.getOutcomeTypeJson();
        return config != null && "degree".equalsIgnoreCase(JsonUtils.getString(config, "name"));
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

    private Degree degreeFor(final AdmissionProcessTarget target) {
        final JsonObject config = target.getOutcomeConfigJson();
        return FenixFramework.getDomainObject(config.get("degree").getAsString());
    }

    private Stream<User> usersFor(final Account account) {
        final Identity identity = account.getIdentity();
        return identity == null ? usersFor(account.getUser()) : Stream.concat(usersFor(identity.getUser()),
                identity.getAccountSet().stream().flatMap(a -> usersFor(a.getUser())));
    }

    private Stream<User> usersFor(final User user) {
        return user == null ? Stream.empty() : Stream.of(user);
    }

    private Set<Degree> possibleDegrees(final Application application) {
        final Account account = application.getAccount();
//        final Identity identity = account.getIdentity();
//        final User user = identity == null ? null : identity.getUser();
//        final Student student = user == null ? null : user.getPerson().getStudent();

//        final Registration registration = student == null ? null : student.getRegistrationsSet().stream()
//                .filter(this::isFirstCycle)
//                .min(Comparator.comparing(this::startDate))
//                .orElse(null);
        final boolean hasPlacement = applicationsFor(account)
                .filter(Application::getAdmitted)
                .map(Application::getAdmissionProcessTarget)
                .anyMatch(target -> isDegreeProcess(target.getAdmissionProcess()));

        final Stream<Degree> possibleStream = hasPlacement ? applicationsFor(account)
                .filter(Application::getAdmitted)
                .map(Application::getAdmissionProcessTarget)
                .filter(target -> isDegreeProcess(target.getAdmissionProcess()))
                .map(this::degreeFor)
                : Stream.concat(
                usersFor(account)
                        .map(User::getPerson)
                        .filter(Objects::nonNull)
                        .map(Person::getStudent)
                        .filter(Objects::nonNull)
                        .flatMap(s -> s.getRegistrationsSet().stream())
                        .filter(Registration::isActive)
                        .flatMap(r -> r.getStudentCurricularPlansSet().stream())
                        .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                        .filter(group -> group.getCycleType() == CycleType.FIRST_CYCLE)
                        .map(group -> group.getDegreeModule().getDegree()),
                usersFor(account)
                        .map(User::getPerson)
                        .filter(Objects::nonNull)
                        .map(Person::getStudent)
                        .filter(Objects::nonNull)
                        .flatMap(s -> s.getRegistrationsSet().stream())
                        .filter(Registration::isActive)
                        .map(Registration::getDegree)
                        .filter(Degree::isFirstCycle));

        final StringBuilder possibleTargetDegreeNames = new StringBuilder();
        return possibleStream
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
                .collect(Collectors.toSet());
    }

    static double calculateCurrentHACSEnrolmentsPendingEvaluation(final Application application) {
        final Identity identity = application.getAccount().getIdentity();
        final User user = identity == null ? null : identity.getUser();
        final Student student = user == null ? null : user.getPerson().getStudent();
        double result = 0d;
        if (student != null) {
            result = student.getRegistrationsSet().stream()
                    .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                    .flatMap(scp -> scp.getRoot().getAllCurriculumGroups().stream())
                    .filter(PlaceHASSApplicationsByOrder::isHASS)
                    .flatMap(PlaceHASSApplicationsByOrder::enrolments)
                    .filter(enrolment -> enrolment.isEnroled() && enrolment.getExecutionPeriod().isCurrent())
                    .mapToDouble(Enrolment::getEctsCredits)
                    .sum();
        }
        return result;
    }

    private static Stream<Enrolment> enrolments(final CurriculumModule module) {
        return module instanceof Enrolment ? Stream.of((Enrolment) module) : module instanceof CurriculumGroup
                ? ((CurriculumGroup) module).getCurriculumModulesSet().stream().flatMap(PlaceHASSApplicationsByOrder::enrolments)
                : Stream.empty();
    }

}