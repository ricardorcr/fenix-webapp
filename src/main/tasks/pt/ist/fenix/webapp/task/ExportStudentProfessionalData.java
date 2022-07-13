package pt.ist.fenix.webapp.task;
 
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
 
import org.apache.commons.lang.WordUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.EctsTableIndex;
import org.fenixedu.academic.domain.degreeStructure.NoEctsComparabilityTableFound;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.StudentDataShareAuthorization;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.util.StudentPersonalDataAuthorizationChoice;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.springframework.util.StringUtils;
 
import com.google.common.base.Strings;
 
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;
 
@SuppressWarnings("deprecation")
@Task(englishTitle = "Export Student Professional Data")
public class ExportStudentProfessionalData extends CustomTask {
 
    public static final String TITLE = "Student Professional Data";
    public static final String FILENAME = TITLE + ".tsv";
    public static final String FULL_NAME = "Name", FIRST_NAME = "First Name", LAST_NAME = "Last Name", GENDER = "Gender",
            DATE_OF_BIRTH = "Date of birth", NATIONALITY = "Nationality", COUNTRY = "Country of residence",
            ADDRESS = "Current address", EMAIL = "Email", PHONE = "Phone", WEBSITE = "Website",
            STUDENT_NUMBER = "IST student number", STUDENT_ID = "IST Student ID", AUTHORIZATION = "Data sharing authorization",
            ROLE = "Current role", AGREEMENT = "Current agreement type", REGISTRATION_STATE = "Last/Current registration state",
            ENTRY_CONDITION = "Last/Current entry condition", INGRESSION_GRADE = "Ingression grade",
            CURRICULAR_YEAR = "Current year of study", BACHELOR_TYPE = "Bachelor degree's type",
            BACHELOR_NAME = "Bachelor degree's programme", BACHELOR_ACRONYM = "Bachelor degree's programme acronym",
            BACHELOR_GRADE = "Bachelor degree's grade", BACHELOR_ECTS_GRADE = "Bachelor degree's ECTS grade",
            BACHELOR_START = "Bachelor degree's start date", BACHELOR_END = "Bachelor degree's end date",
            BACHELOR_ECTS = "Bachelor degree's ECTS", MASTER_TYPE = "Master degree's type",
            MASTER_NAME = "Master degree's programme", MASTER_ACRONYM = "Master degree's programme acronym",
            MASTER_SPECIALIZATION = "Master degree's specialization(s)", MASTER_GRADE = "Master degree's grade",
            MASTER_ECTS_GRADE = "Master degree's ects grade", MASTER_START = "Master degree's start date",
            MASTER_END = "Master degree's end date", MASTER_ECTS = "Master degree's ECTS",
            AD_TYPE = "Advanced diploma degree's type", AD_NAME = "Advanced diploma degree's programme",
            AD_ACRONYM = "Advanced diploma degree's programme acronym", AD_GRADE = "Advanced diploma degree's grade",
            AD_ECTS_GRADE = "Advanced diploma degree's ects grade", AD_START = "Advanced diploma degree's start date",
            AD_END = "Advanced diploma degree's end date", AD_ECTS = "Advanced diploma degree's ECTS",
            PHD_NAME = "PhD programme name", PHD_ACRONYM = "PhD programme acronym", PHD_TITLE = "PhD thesis title",
            PHD_SUPERVISOR = "PhD thesis supervisor", PHD_GRADE = "PhD programme grade", PHD_START = "PhD start date",
            PHD_END = "PhD end date", IMPORT_DATE = "Last import date", NOTES = "Notes",
            LAST_AUTHORIZATION = "Last Data sharing authorization",
            LAST_AUTHORIZATION_DATE = "Last Data sharing authorization date";
    public static final String[] ORDERED_FIELDS =
            { STUDENT_ID, STUDENT_NUMBER, FIRST_NAME, LAST_NAME, FULL_NAME, GENDER, DATE_OF_BIRTH, NATIONALITY, COUNTRY, ADDRESS,
                    PHONE, EMAIL, WEBSITE, AUTHORIZATION, ROLE, AGREEMENT, REGISTRATION_STATE, ENTRY_CONDITION, INGRESSION_GRADE,
                    CURRICULAR_YEAR, BACHELOR_TYPE, BACHELOR_NAME, BACHELOR_ACRONYM, BACHELOR_GRADE, BACHELOR_ECTS_GRADE,
                    BACHELOR_START, BACHELOR_END, BACHELOR_ECTS, MASTER_TYPE, MASTER_NAME, MASTER_ACRONYM, MASTER_SPECIALIZATION,
                    MASTER_GRADE, MASTER_ECTS_GRADE, MASTER_START, MASTER_END, MASTER_ECTS, AD_TYPE, AD_NAME, AD_ACRONYM,
                    AD_GRADE, AD_ECTS_GRADE, AD_START, AD_END, AD_ECTS, PHD_NAME, PHD_ACRONYM, PHD_TITLE, PHD_SUPERVISOR,
                    PHD_GRADE, PHD_START, PHD_END, IMPORT_DATE, NOTES, LAST_AUTHORIZATION, LAST_AUTHORIZATION_DATE },
            DEGREE_TYPE = { BACHELOR_TYPE, MASTER_TYPE, AD_TYPE }, DEGREE_NAME = { BACHELOR_NAME, MASTER_NAME, AD_NAME },
            DEGREE_ACRONYM = { BACHELOR_ACRONYM, MASTER_ACRONYM, AD_ACRONYM },
            DEGREE_GRADE = { BACHELOR_GRADE, MASTER_GRADE, AD_GRADE },
            DEGREE_ECTS_GRADE = { BACHELOR_ECTS_GRADE, MASTER_ECTS_GRADE, AD_ECTS_GRADE },
            DEGREE_START = { BACHELOR_START, MASTER_START, AD_START }, DEGREE_END = { BACHELOR_END, MASTER_END, AD_END },
            DEGREE_ECTS = { BACHELOR_ECTS, MASTER_ECTS, AD_ECTS };
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final List<CycleType> relevantCycles;
    static {
        List<CycleType> cycles = new ArrayList<CycleType>();
        cycles.add(CycleType.FIRST_CYCLE);
        cycles.add(CycleType.SECOND_CYCLE);
        cycles.add(CycleType.THIRD_CYCLE);
        relevantCycles = Collections.unmodifiableList(cycles);
    }
    /* XXX the following ugly piece of code is to reduce code changes when the bugged row.setCell(FIELD_IDX.get(String header), String cellValue) is fixed.
     * Just remove it and replace 'FIELD_IDX.get\\((.*?)\\)' matches with '\\1'.
     */
    public static final Map<String, Integer> FIELD_IDX;
    static {
        Map<String, Integer> fields = new HashMap<String, Integer>();
        for (int i = 0; i < ORDERED_FIELDS.length; i++) {
            fields.put(ORDERED_FIELDS[i], i);
        }
        FIELD_IDX = Collections.unmodifiableMap(fields);
    }
 
    private static final Comparator<PhdIndividualProgramProcess> PHD_COMPARATOR_BY_DESC_START =
            new Comparator<PhdIndividualProgramProcess>() {
                @Override
                public int compare(PhdIndividualProgramProcess o1, PhdIndividualProgramProcess o2) {
                    return o2.getWhenStartedStudies().compareTo(o1.getWhenStartedStudies());
                }
            };
 
    private static final Comparator<Registration> REG_COMPARATOR_BY_DESC_START =
            Collections.reverseOrder(Registration.COMPARATOR_BY_START_DATE);
 
    private Spreadsheet spreadsheet;
    private DateTime exportTimeStamp, minAuthDate;
    private YearMonthDay minConclusionDate;
    private double progress, universe, step;
 
    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }
 
    private static String tsvSafe(String address) {
        return Strings.isNullOrEmpty(address) ? "" : address.replaceAll("\\\\s+", " ");
    }
 
    private static void addPersonalData(Row row, Student student, DateTime minAuthDate) {
        Person person = student.getPerson();
        UserProfile profile = person.getProfile();
        row.setCell(FIELD_IDX.get(STUDENT_ID), person.getUsername());
        row.setCell(FIELD_IDX.get(STUDENT_NUMBER), student.getStudentNumber().getNumber().toString());
        row.setCell(FIELD_IDX.get(FIRST_NAME), profile.getGivenNames());
        row.setCell(FIELD_IDX.get(LAST_NAME), profile.getFamilyNames());
        row.setCell(FIELD_IDX.get(FULL_NAME), profile.getFullName());
        row.setCell(FIELD_IDX.get(GENDER), person.getGender().toLocalizedString());
        YearMonthDay dob = person.getDateOfBirthYearMonthDay();
        if (dob != null) {
            row.setCell(FIELD_IDX.get(DATE_OF_BIRTH), dob.toString(DATE_FORMAT));
        }
        Country country = person.getCountry();
        if (country != null) {
            row.setCell(FIELD_IDX.get(NATIONALITY),
                    StringUtils.capitalize(country.getCountryNationality().getContent().toLowerCase()));
        }
        country = person.getCountryOfResidence();
        if (country != null) {
            row.setCell(FIELD_IDX.get(COUNTRY), StringUtils.capitalize(country.getName().toLowerCase()));
        }
        row.setCell(FIELD_IDX.get(ADDRESS), tsvSafe(person.getAddress()));
        row.setCell(FIELD_IDX.get(PHONE), person.getDefaultMobilePhoneNumber());
        row.setCell(FIELD_IDX.get(EMAIL), person.getDefaultEmailAddressValue());
        row.setCell(FIELD_IDX.get(WEBSITE), person.getDefaultWebAddressUrl());
        StudentDataShareAuthorization auth = student.getActivePersonalDataAuthorization();
        boolean valid_auth = auth != null && auth.getSince().isAfter(minAuthDate)
                && auth.getAuthorizationChoice() != StudentPersonalDataAuthorizationChoice.STUDENTS_ASSOCIATION;
        StudentPersonalDataAuthorizationChoice choice =
                valid_auth ? auth.getAuthorizationChoice() : StudentPersonalDataAuthorizationChoice.NO_END;
        row.setCell(FIELD_IDX.get(AUTHORIZATION), choice.getName().toLowerCase().replace("_", " "));
        row.setCell(FIELD_IDX.get(LAST_AUTHORIZATION),
                (auth == null ? StudentPersonalDataAuthorizationChoice.NO_END : auth.getAuthorizationChoice()).getName()
                        .toLowerCase().replace("_", " "));
        row.setCell(FIELD_IDX.get(LAST_AUTHORIZATION_DATE), auth == null ? "" : auth.getSince().toString("yyyy-MM-dd HH:mm"));
    }
 
    private static void addCycleCurriculumData(Row row, CycleType type, Registration registration) {
        if (registration != null) {
            if (type.equals(CycleType.FIRST_CYCLE)) {
                Double ingressionGrade = registration.getEntryGrade();
                if (ingressionGrade != null) {
                    row.setCell(FIELD_IDX.get(INGRESSION_GRADE), String.format(Locale.US, "%.2f", ingressionGrade / 10));
                }
            }
            int typeIdx = type.getWeight() - 1;
            CycleCurriculumGroup cycle = registration.getStudentCurricularPlansSet().stream()
                    .sorted(StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_START_DATE.reversed())
                    .map(scp -> scp.getCycle(type)).filter(Objects::nonNull).findFirst().orElse(null);
            Degree degree = registration.getDegree();
            YearMonthDay start = registration.getStartDate();
            YearMonthDay conclusion = null;
            boolean isConcluded = false;
            try {
                isConcluded = cycle.isConcluded();
            } catch (DomainException e) {
                //FIXME for anomalous situations where multiple credit limit rules break CycleCurriculumGroup::isConcluded
            }
            if (isConcluded) {
                conclusion = cycle.getConclusionDate();
                if (conclusion == null) {
                    conclusion = cycle.calculateConclusionDate();
                }
                Grade grade = cycle.calculateFinalGrade();
                row.setCell(FIELD_IDX.get(DEGREE_GRADE[typeIdx]), grade.getValue());
                try {
                    row.setCell(FIELD_IDX.get(DEGREE_ECTS_GRADE[typeIdx]),
                            EctsTableIndex.getGraduationGradeConversionTable(degree, type,
                                    ExecutionYear.getExecutionYearByDate(conclusion).getAcademicInterval(), DateTime.now())
                                    .convert(grade).getValue());
                } catch (NoEctsComparabilityTableFound e) {
                } catch (DomainException e) {
                }
                if (conclusion != null) {
                    row.setCell(FIELD_IDX.get(DEGREE_END[typeIdx]), conclusion.toString(DATE_FORMAT));
                }
            } else {
                row.setCell(FIELD_IDX.get(DEGREE_GRADE[typeIdx]), cycle.calculateRawGrade().getValue());
            }
            DegreeType degreeType = degree.getDegreeType();
            row.setCell(FIELD_IDX.get(DEGREE_TYPE[typeIdx]), degreeType.getName().getContent());
            row.setCell(FIELD_IDX.get(DEGREE_NAME[typeIdx]), (conclusion != null ? degree.getPresentationName(
                    ExecutionYear.readByDateTime(conclusion.toDateTimeAtMidnight())) : degree.getPresentationName()));
            row.setCell(FIELD_IDX.get(DEGREE_ACRONYM[typeIdx]), degree.getSigla());
            if (type.equals(CycleType.SECOND_CYCLE)) {
                row.setCell(FIELD_IDX.get(MASTER_SPECIALIZATION), cycle.getBranchCurriculumGroups().stream()
                        .map(g -> g.getName().getContent()).collect(Collectors.joining(".")));
                if (degreeType.isIntegratedMasterDegree() && !cycle.getEnrolments().isEmpty()) {
                    YearMonthDay firstEnrolment = Collections.min(cycle.getEnrolments(), Enrolment.COMPARATOR_BY_EXECUTION_PERIOD)
                            .getExecutionPeriod().getBeginDateYearMonthDay();
                    if (firstEnrolment.isAfter(start)) {
                        start = firstEnrolment;
                    }
                }
            }
            row.setCell(FIELD_IDX.get(DEGREE_START[typeIdx]), start.toString(DATE_FORMAT));
            row.setCell(FIELD_IDX.get(DEGREE_ECTS[typeIdx]), registration.getCurriculum(type).getSumEctsCredits().toString());
        }
    }
 
    private void makeRecord(Student student) {
        Row row;
        synchronized (spreadsheet) {
            row = spreadsheet.addRow();
        }
 
        FenixFramework.atomic(() -> {
            addPersonalData(row, student, minAuthDate);
            addLastActivityStatus(row, student);
 
            PhdIndividualProgramProcess detailedPhd = relevantPhds(student).findFirst().orElse(null);
            Map<CycleType, Registration> detailedRegistrations = new HashMap<CycleType, Registration>();
            if (detailedPhd != null) {
                Registration r = detailedPhd.getRegistration();
                if (r != null && r.getDegree().isThirdCycle()) {//FIXME for anomalous cases where phd registration is not third cycle
                    detailedRegistrations.put(CycleType.THIRD_CYCLE, r);
                }
                addPhdCuriculumData(row, detailedPhd);
            }
            relevantCycles.forEach(ct -> detailedRegistrations.computeIfAbsent(ct,
                    k -> relevantRegistrations(student).filter(r -> hasCycle(r, ct)).findFirst().orElse(null)));
            detailedRegistrations.forEach((ct, r) -> addCycleCurriculumData(row, ct, r));
 
            String notes = relevantRegistrations(student).filter(r -> !detailedRegistrations.values().contains(r))
                    .map(this::getBasicRegistrationData).collect(Collectors.joining(" "));
            notes += relevantPhds(student).filter(p -> !p.equals(detailedPhd)).map(ExportStudentProfessionalData::getBasicPhdData)
                    .collect(Collectors.joining(" "));
            if (!notes.isEmpty()) {
                notes = "Outros Cursos: " + notes;
            }
            row.setCell(FIELD_IDX.get(NOTES), notes);
            row.setCell(FIELD_IDX.get(IMPORT_DATE), exportTimeStamp.toString(DATE_FORMAT));
        });
        showDotProgress();
    }
 
    private void addPhdCuriculumData(Row row, PhdIndividualProgramProcess detailedPhd) {
        String title = detailedPhd.getThesisTitleEn();
        if (Strings.isNullOrEmpty(title)) {
            title = detailedPhd.getThesisTitle();
        }
        PhdProgram program = detailedPhd.getPhdProgram();
        row.setCell(FIELD_IDX.get(PHD_NAME), program.getPresentationName());
        row.setCell(FIELD_IDX.get(PHD_ACRONYM), program.getAcronym());
        row.setCell(FIELD_IDX.get(PHD_TITLE), WordUtils.capitalizeFully(title));
        row.setCell(FIELD_IDX.get(PHD_SUPERVISOR),
                detailedPhd.getGuidingsSet().stream().map(p -> p.getName()).collect(Collectors.joining(", ")));
        LocalDate start = detailedPhd.getWhenStartedStudies();
        if (start != null) {
            row.setCell(FIELD_IDX.get(PHD_START), start.toString(DATE_FORMAT));
        }
        if (detailedPhd.isConcluded()) {
            row.setCell(FIELD_IDX.get(PHD_END), detailedPhd.getConclusionDate().toString(DATE_FORMAT));
            row.setCell(FIELD_IDX.get(PHD_GRADE), detailedPhd.getFinalGrade().getLocalizedName());
        }
    }
 
    private static void addLastActivityStatus(Row row, Student student) {
        boolean active = true;
        PhdIndividualProgramProcess lastPhd =
                relevantPhds(student).filter(p -> student.isValidAndActivePhdProcess(p)).findFirst().orElse(null);
        Registration lastRegistration = relevantRegistrations(student).filter(Registration::isActive).findFirst().orElse(null);
 
        if (lastPhd == null && lastRegistration == null) {
            active = false;
            lastPhd = relevantPhds(student).filter(PhdIndividualProgramProcess::isConcluded).findFirst().orElse(null);
            lastRegistration = relevantRegistrations(student).filter(r -> !r.isActive()).findFirst().orElse(null);
        }
 
        if (lastPhd != null && lastRegistration != null) {
            //XXX keep consistency in cases where most recent registration does not match most recent PhD
            if (lastPhd.getWhenStartedStudies().isAfter(lastRegistration.getStartDate())) {
                lastRegistration = lastPhd.getRegistration();
            } else {
                Registration r = lastPhd.getRegistration();
                if (r != null && r.equals(lastRegistration)) {
                    lastPhd = null;
                }
            }
        }
 
        row.setCell(FIELD_IDX.get(ROLE), active ? "Student" : "Alumni");
        if (lastRegistration != null) {
            if (active) {
                row.setCell(FIELD_IDX.get(AGREEMENT), lastRegistration.getRegistrationProtocol().getDescription().getContent());
            }
            if (lastRegistration.getIngressionType() != null) {
                row.setCell(FIELD_IDX.get(ENTRY_CONDITION), lastRegistration.getIngressionType().getDescription().getContent());
            }
            row.setCell(FIELD_IDX.get(CURRICULAR_YEAR), "" + lastRegistration.getCurricularYear());
        }
 
        if (lastPhd != null) {
            row.setCell(FIELD_IDX.get(REGISTRATION_STATE), lastPhd.getActiveState().getLocalizedName());
        } else if (lastRegistration != null) {
            row.setCell(FIELD_IDX.get(REGISTRATION_STATE), lastRegistration.getActiveStateType().getDescription());
        }
 
    }
 
    private String getBasicRegistrationData(Registration r) {
        YearMonthDay conclusion = getRegistrationConclusionDate(r);
        if (conclusion != null) {
            Grade grade = r.getFinalGrade();
            return r.getDegree().getPresentationName(ExecutionYear.readByDateTime(conclusion.toDateTimeAtMidnight()))
                    + ", started in " + r.getStartDate().toString(DATE_FORMAT) + " and concluded in "
                    + conclusion.toString(DATE_FORMAT) + " with a grade of "
                    + (grade != null ? grade : r.calculateRawGrade()).getValue() + ".";
        } else {
            return r.getDegree().getPresentationName() + ", started in " + r.getStartDate().toString(DATE_FORMAT)
                    + ", currrently in the " + r.getActiveStateType().getDescription() + " state, with an average grade of "
                    + r.calculateRawGrade().getValue() + " in " + r.getCurriculum().getSumEctsCredits() + " approved ECTS.";
        }
    }
 
    private static String getBasicPhdData(PhdIndividualProgramProcess p) {
        String line = "PhD Thesis " + p.getThesisTitleEn();
        if (p.isConcluded()) {
            line += ", concluded in " + p.getConclusionDate().toString(DATE_FORMAT) + " and valued as "
                    + p.getFinalGrade().getLocalizedName();
        }
        line += " under the supervision of "
                + p.getGuidingsSet().stream().map(s -> s.getNameWithTitle()).collect(Collectors.joining(", "));
        return line + ".";
    }
 
    private Stream<Student> getSelectedStudents() {
        final Set<Student> students = Bennu.getInstance().getStudentsSet();
        resetProgress("Filtering %.0f students", students.size());
        return students.stream().filter(this::studentFilter);
    }
 
    private boolean studentFilter(Student student) {
        boolean[] result = new boolean[1];
        FenixFramework.atomic(() -> {
            result[0] = student.getPerson() != null //FIXME temporarily to deal with anomalous students like 171798707515
                    && (isActiveStudent(student) || isRecentAlumni(student));
            showDotProgress();
        });
        return result[0];
    }
 
    private boolean isActiveStudent(Student student) {
        return student.hasActivePhdProgramProcess() || hasActiveRegistrations(student);
    }
 
    private static boolean hasActiveRegistrations(Student student) {
        return relevantRegistrations(student).anyMatch(Registration::isActive);
    }
 
    private static Stream<Registration> relevantRegistrations(Student student) {
        return student.getRegistrationsSet().stream().filter(ExportStudentProfessionalData::hasRelevantCycles)
                .filter(ExportStudentProfessionalData::hasRelevantState).sorted(REG_COMPARATOR_BY_DESC_START);
    }
 
    private static Stream<PhdIndividualProgramProcess> relevantPhds(Student student) {
        return student.getPerson().getPhdIndividualProgramProcessesSet().stream()
                .filter(p -> student.isValidAndActivePhdProcess(p) || p.isConcluded()).sorted(PHD_COMPARATOR_BY_DESC_START);
    }
 
    private static boolean hasRelevantCycles(Registration registration) {
        return getRelevantCycles(registration).findAny().isPresent();
    }
 
    private static Stream<CycleCurriculumGroup> getRelevantCycles(Registration registration) {
        return registration.getDegreeType().getCycleTypes().stream().filter(t -> relevantCycles.contains(t))
                .flatMap(t -> registration.getStudentCurricularPlansSet().stream()
                        .sorted(StudentCurricularPlan.STUDENT_CURRICULAR_PLAN_COMPARATOR_BY_START_DATE.reversed())
                        .map(scp -> scp.getCycle(t)))
                .filter(Objects::nonNull);
    }
 
    private static boolean hasRelevantState(Registration registration) {
        RegistrationStateType type = registration.getActiveStateType();
        return type.isActive() || type.equals(RegistrationStateType.CONCLUDED)
                || type.equals(RegistrationStateType.SCHOOLPARTCONCLUDED)
                || type.equals(RegistrationStateType.STUDYPLANCONCLUDED);
    }
 
    private static boolean hasCycle(Registration registration, CycleType cycleType) {
        return registration.getDegreeType().getCycleTypes().contains(cycleType)
                && registration.getStudentCurricularPlansSet().stream().anyMatch(scp -> scp.getCycle(cycleType) != null);
    }
 
    private boolean isRecentAlumni(Student student) {
        return hasRecentlyConcludedRegistrations(student) || hasRecentlyConcludedPhdProgram(student);
    }
 
    private boolean hasRecentlyConcludedRegistrations(Student student) {
        return relevantRegistrations(student).anyMatch(this::isRegistrationRecentlyConcluded);
    }
 
    private boolean isRegistrationRecentlyConcluded(Registration registration) {
        YearMonthDay conclusion = getRegistrationConclusionDate(registration);
        return conclusion != null && conclusion.isAfter(minConclusionDate);
    }
 
    private YearMonthDay getRegistrationConclusionDate(Registration registration) {
        YearMonthDay result = registration.getConclusionDate();
        if (result == null) {
            result = getRelevantCycles(registration).filter(g -> {
                try {
                    return g.isConcluded();
                } catch (DomainException e) {
                    return false; //FIXME for anomalous situations where multiple credit limit rules break CycleCurriculumGroup::isConcluded
                }
            }).map(g -> {
                YearMonthDay date = g.getConclusionDate();
                if (date == null) {
                    date = g.calculateConclusionDate();
                }
                return date;
            }).sorted(Collections.reverseOrder()).findFirst().orElse(null);
        }
        return result;
    }
 
    private boolean hasRecentlyConcludedPhdProgram(Student student) {
        return student.getPerson().getPhdIndividualProgramProcessesSet().stream()
                .anyMatch(PhdIndividualProgramProcess::isConcluded);
    }
 
    private void resetProgress(String task, double universe) {
        resetProgress(task, universe, 0.01);
    }
 
    private void resetProgress(String task, double universe, double step) {
        this.universe = universe;
        this.progress = 0;
        this.step = step;
        taskLog("\n" + task + "\n", this.universe, this.progress, this.step);
    }
 
    private void showDotProgress() {
        progress++;
        if (progress / universe > this.step) {
            taskLog("%s", ".");
            progress -= universe * this.step;
        }
    }
 
    @Override
    public void runTask() throws Exception {
        I18N.setLocale(new Locale("en", "GB"));
        exportTimeStamp = new DateTime();
        minAuthDate = exportTimeStamp.minusYears(1);
        minConclusionDate = exportTimeStamp.minusYears(2).toYearMonthDay();
        spreadsheet = new Spreadsheet(FILENAME);
        spreadsheet.setHeaders(ORDERED_FIELDS);
        final Stream<Student> students = getSelectedStudents();
        taskLog("Processing students");
        students.parallel().forEach(this::makeRecord);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spreadsheet.exportToCSV(out, "\t");
        output(FILENAME, out.toByteArray());
    }
}