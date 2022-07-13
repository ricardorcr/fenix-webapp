package pt.ist.fenix.tasks;
 
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
 
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.StudentDataShareAuthorization;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.StudentPersonalDataAuthorizationChoice;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.i18n.BundleUtil;
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
@Task(englishTitle = "Export Alumni Professional Data")
public class ExportAlumniProfessionalData extends CustomTask {
 
    public static final String TITLE = "Alumni Professional Data";
    public static final String FILENAME = TITLE + ".tsv";
    public static final String FULL_NAME = "Name", FIRST_NAME = "First Name", LAST_NAME = "Last Name", GENDER = "Gender",
            DATE_OF_BIRTH = "Date of birth", NATIONALITY = "Nationality", COUNTRY = "Country of residence",
            ADDRESS = "Current address", EMAIL = "Email", PHONE = "Phone", WEBSITE = "Website",
            STUDENT_NUMBER = "IST student number", STUDENT_ID = "IST Student ID", AUTHORIZATION = "Data sharing authorization",
            ENTRY_CONDITION = "Entry condition", DEGREE_TYPE = "Degree's type", DEGREE_NAME = "Degree's programme",
            DEGREE_ACRONYM = "Degree's programme acronym", DEGREE_CYCLE = "Degree's cycle", DEGREE_GRADE = "Degree's grade",
            DEGREE_START = "Degree's start date", DEGREE_END = "Degree's end date", IMPORT_DATE = "Last import date",
            LAST_AUTHORIZATION = "Last Data sharing authorization",
            LAST_AUTHORIZATION_DATE = "Last Data sharing authorization date";
    public static final String[] ORDERED_FIELDS =
            { STUDENT_ID, STUDENT_NUMBER, FIRST_NAME, LAST_NAME, FULL_NAME, GENDER, DATE_OF_BIRTH, NATIONALITY, COUNTRY, ADDRESS,
                    PHONE, EMAIL, WEBSITE, AUTHORIZATION, ENTRY_CONDITION, DEGREE_TYPE, DEGREE_NAME, DEGREE_ACRONYM, DEGREE_CYCLE,
                    DEGREE_GRADE, DEGREE_START, DEGREE_END, IMPORT_DATE, LAST_AUTHORIZATION, LAST_AUTHORIZATION_DATE };
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String PHD_TYPE = BundleUtil.getString(Bundle.PHD, "label.php.program");
    public static final List<CycleType> relevantCycles;
 
    static {
        List<CycleType> cycles = new ArrayList<CycleType>();
        cycles.add(CycleType.FIRST_CYCLE);
        cycles.add(CycleType.SECOND_CYCLE);
        cycles.add(CycleType.THIRD_CYCLE);
        relevantCycles = Collections.unmodifiableList(cycles);
    }
 
    /* XXX the following ugly piece of code is to reduce code changes
    when the bugged row.setCell(FIELD_IDX.get(String header), String
    cellValue) is fixed.
     * Just remove it and replace 'FIELD_IDX.get\\((.*?)\\)' matches with
    '\\1'.
     */
    public static final Map<String, Integer> FIELD_IDX;
 
    static {
        Map<String, Integer> fields = new HashMap<String, Integer>();
        for (int i = 0; i < ORDERED_FIELDS.length; i++) {
            fields.put(ORDERED_FIELDS[i], i);
        }
        FIELD_IDX = Collections.unmodifiableMap(fields);
    }
 
    private Spreadsheet spreadsheet;
    private final DateTime exportTimeStamp = new DateTime(), minAuthDate = exportTimeStamp.minusYears(1);
    private double progress, universe, step;
 
    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }
 
    private void makeRecord(Registration registration) {
        FenixFramework.atomic(() -> {
            Map<Integer, String> commonFields = getCommonInformation(registration.getStudent()),
                    registrationFields = getRegistrationInformation(registration);
            getRelevantCycles(registration).forEach(c -> {
                Row row;
                synchronized (spreadsheet) {
                    row = spreadsheet.addRow();
                }
                commonFields.forEach(row::setCell);
                registrationFields.forEach(row::setCell);
                addRegistrationCycleData(row, c);
            });
            showDotProgress();
        });
    }
 
    private Map<Integer, String> getCommonInformation(Student student) {
        Map<Integer, String> cells = new HashMap<>();
        Person person = student.getPerson();
        UserProfile profile = person.getProfile();
        cells.put(FIELD_IDX.get(STUDENT_ID), person.getUsername());
        cells.put(FIELD_IDX.get(STUDENT_NUMBER), student.getStudentNumber().getNumber().toString());
        cells.put(FIELD_IDX.get(FIRST_NAME), profile.getGivenNames());
        cells.put(FIELD_IDX.get(LAST_NAME), profile.getFamilyNames());
        cells.put(FIELD_IDX.get(FULL_NAME), profile.getFullName());
        cells.put(FIELD_IDX.get(GENDER), person.getGender().getLocalizedName());
        YearMonthDay dob = person.getDateOfBirthYearMonthDay();
        if (dob != null) {
            cells.put(FIELD_IDX.get(DATE_OF_BIRTH), dob.toString(DATE_FORMAT));
        }
        Country country = person.getCountry();
        if (country != null) {
            cells.put(FIELD_IDX.get(NATIONALITY),
 
            StringUtils.capitalize(country.getCountryNationality().getContent().toLowerCase()));
        }
        country = person.getCountryOfResidence();
        if (country != null) {
            cells.put(FIELD_IDX.get(COUNTRY), StringUtils.capitalize(country.getName().toLowerCase()));
        }
        cells.put(FIELD_IDX.get(ADDRESS), tsvSafe(person.getAddress()));
        cells.put(FIELD_IDX.get(PHONE), person.getDefaultMobilePhoneNumber());
        cells.put(FIELD_IDX.get(EMAIL), person.getDefaultEmailAddressValue());
        cells.put(FIELD_IDX.get(WEBSITE), person.getDefaultWebAddressUrl());
        StudentDataShareAuthorization auth = student.getActivePersonalDataAuthorization();
        boolean valid_auth = auth != null && auth.getSince().isAfter(minAuthDate)
                && auth.getAuthorizationChoice() != StudentPersonalDataAuthorizationChoice.STUDENTS_ASSOCIATION;
        StudentPersonalDataAuthorizationChoice choice =
                valid_auth ? auth.getAuthorizationChoice() : StudentPersonalDataAuthorizationChoice.NO_END;
        cells.put(FIELD_IDX.get(AUTHORIZATION), choice.getName().toLowerCase().replace("_", " "));
        cells.put(FIELD_IDX.get(IMPORT_DATE), exportTimeStamp.toString(DATE_FORMAT));
        cells.put(FIELD_IDX.get(LAST_AUTHORIZATION),
                (auth == null ? StudentPersonalDataAuthorizationChoice.NO_END : auth.getAuthorizationChoice()).getName()
                        .toLowerCase().replace("_", " "));
        cells.put(FIELD_IDX.get(LAST_AUTHORIZATION_DATE), auth == null ? "" : auth.getSince().toString("yyyy-MM-dd HH:mm"));
        return cells;
    }
 
    private static String tsvSafe(String address) {
        return Strings.isNullOrEmpty(address) ? "" : address.replaceAll("\\\\s+", " ");
    }
 
    private Map<Integer, String> getRegistrationInformation(Registration registration) {
        Map<Integer, String> cells = new HashMap<>();
        YearMonthDay end = registration.getConclusionDate();
        if (end == null) {
            end = registration.calculateConclusionDate();
        }
        IngressionType ingression = registration.getIngressionType();
        if (ingression != null) {
            cells.put(FIELD_IDX.get(ENTRY_CONDITION), ingression.getDescription().getContent());
        }
        cells.put(FIELD_IDX.get(DEGREE_TYPE), registration.getDegreeType().getName().getContent());
 
        cells.put(FIELD_IDX.get(DEGREE_NAME), registration.getDegreeName());
        cells.put(FIELD_IDX.get(DEGREE_ACRONYM), registration.getDegree().getSigla());
        cells.put(FIELD_IDX.get(DEGREE_START), registration.getStartDate().toString(DATE_FORMAT));
        return cells;
    }
 
    private static void addRegistrationCycleData(Row row, CycleCurriculumGroup cycle) {
        row.setCell(FIELD_IDX.get(DEGREE_CYCLE), cycle.getCycleType().getDescription());
        row.setCell(FIELD_IDX.get(DEGREE_GRADE), cycle.calculateFinalGrade().getValue());
        YearMonthDay end = cycle.getConclusionDate();
        if (end == null) {
            end = cycle.calculateConclusionDate();
        }
        if (end != null) {
            row.setCell(FIELD_IDX.get(DEGREE_END), end.toString(DATE_FORMAT));
        }
    }
 
    private void makeRecord(PhdIndividualProgramProcess phd) {
        Row row = spreadsheet.addRow();
        getCommonInformation(phd.getStudent()).forEach((i, c) -> row.setCell(i, c));
        addPhdCurriculumData(row, phd);
        showDotProgress();
    }
 
    private static void addPhdCurriculumData(Row row, PhdIndividualProgramProcess phd) {
        String title = phd.getThesisTitleEn();
        if (Strings.isNullOrEmpty(title)) {
            title = phd.getThesisTitle();
        }
        PhdProgram program = phd.getPhdProgram();
        row.setCell(FIELD_IDX.get(DEGREE_TYPE), PHD_TYPE);
        row.setCell(FIELD_IDX.get(DEGREE_NAME), program.getPresentationName());
        row.setCell(FIELD_IDX.get(DEGREE_ACRONYM), program.getAcronym());
        row.setCell(FIELD_IDX.get(DEGREE_GRADE), phd.getFinalGrade().getLocalizedName());
        LocalDate start = phd.getWhenStartedStudies();
        if (start != null) {
            row.setCell(FIELD_IDX.get(DEGREE_START), start.toString(DATE_FORMAT));
        }
        row.setCell(FIELD_IDX.get(DEGREE_END), phd.getConclusionDate().toString(DATE_FORMAT));
    }
 
    private Stream<Registration> getConcludedRegistrations() {
        final Set<Registration> registrations = Bennu.getInstance().getRegistrationsSet();
        resetProgress("Filtering %.0f registrations", registrations.size());
        return registrations.stream().filter(this::concludedRegistrationFilter);
    }
 
    private boolean concludedRegistrationFilter(Registration registration) {
        boolean result[] = new boolean[1];
        try {
            FenixFramework.atomic(() -> result[0] = getRelevantCycles(registration).findAny().isPresent());
        } catch (Exception e) {
            throw new Error(e);
        }
        showDotProgress();
        return result[0];
    }
 
    private static Stream<CycleCurriculumGroup> getRelevantCycles(Registration registration) {
        return registration.getDegreeType().getCycleTypes().stream()
                .filter(t -> relevantCycles.contains(t))
                .flatMap(t -> registration.getStudentCurricularPlansSet().stream().map(scp -> scp.getCycle(t)))
                .filter(Objects::nonNull)
                .filter(g -> {
                  try {
                      return g.isConcluded();
                  } catch (DomainException e) {
                      return false;//XXX for anomalous situations where multiple credit limit rules break CycleCurriculumGroup::isConcluded
                  }
              });
    }
 
    private Stream<PhdIndividualProgramProcess> getConcludedPhds() {
        Stream<PhdIndividualProgramProcess> processes =
        Bennu.getInstance().getPhdProgramsSet().stream().flatMap(p -> p.getIndividualProgramProcessesSet().stream());
        taskLog("Filtering PhDs");
        return processes.filter(p -> {
            showDotProgress();
            return p.isConcluded();
        });
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
        spreadsheet = new Spreadsheet(FILENAME);
        spreadsheet.setHeaders(ORDERED_FIELDS);
        final Stream<Registration> registrations = getConcludedRegistrations();
        resetProgress("Processing %.0f registrations", Bennu.getInstance().getRegistrationsSet().size());
        registrations.parallel().forEach(this::makeRecord);
        Stream<PhdIndividualProgramProcess> phds = getConcludedPhds();
        taskLog("Processing phds");
        phds.forEach(this::makeRecord);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spreadsheet.exportToCSV(out, "\t");
        output(FILENAME, out.toByteArray());
    }
 
}