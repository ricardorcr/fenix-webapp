package pt.ist.fenix.webapp.task.academic.schedules;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EntryPhase;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import com.google.common.base.Joiner;

import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class CreateExecutionCourses extends CustomTask {

	@Override
	public TxMode getTxMode() {
		return TxMode.READ;
	}

	protected ExecutionSemester originExecutionSemester;
	protected ExecutionSemester destinationExecutionSemester;

	private Spreadsheet spreadsheet;

	protected Set<CurricularCourse> processedCurricularCourses = new HashSet<CurricularCourse>();

	protected Map<SchoolClass, SchoolClass> schoolClassTranslation = new HashMap<SchoolClass, SchoolClass>();

	private LocalizedString evaluationMethod = new LocalizedString.Builder().with(Locale.forLanguageTag("en-GB"), " ").with(LocaleUtils.PT, " ").build();

	@Override
	public void runTask() throws Exception {

		FenixFramework.atomic(this::create);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		spreadsheet.exportToXLSSheet(baos);
		String filename = new DateTime().toString("YYYY_MM_dd_HH_mm") + "_executioncourses_" + destinationExecutionSemester.getQualifiedName().replaceAll(" ", "_").replaceAll("/", "_") + ".xls";
		output(filename, baos.toByteArray());

		printReport();
	}

	protected void create() {
		processedCurricularCourses.clear();
		schoolClassTranslation.clear();

		ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
		ExecutionYear nextExecutionYear = currentExecutionYear.getNextExecutionYear();

		originExecutionSemester = currentExecutionYear.getFirstExecutionPeriod();
		destinationExecutionSemester = nextExecutionYear.getFirstExecutionPeriod();

		spreadsheet = new Spreadsheet("schedule_copy");

		printExecutionSemesterInfo();

		createSchoolClasses();

		loadExistingCodes();

		for (final ExecutionCourse executionCourse : originExecutionSemester.getAssociatedExecutionCoursesSet()) {
			final EntryPhase entryPhase = executionCourse.getEntryPhase();
			if (entryPhase == null || EntryPhase.FIRST_PHASE.equals(entryPhase)) {
				createFromPreviouse(executionCourse);
			}
		}

		final ExecutionYear executionYear = destinationExecutionSemester.getExecutionYear();
		for (final ExecutionDegree executionDegree : executionYear.getExecutionDegreesSet()) {
			final DegreeCurricularPlan degreeCurricularPlan = executionDegree.getDegreeCurricularPlan();
			for (final CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
				if (!curricularCourse.isOptional() && shouldCreateFor(curricularCourse)) {
					createFor(curricularCourse);
				}
			}
		}
	}

	private void createSchoolClasses() {
		final ExecutionYear executionYear = originExecutionSemester.getExecutionYear();
		for (final ExecutionDegree originalExecutionDegree : executionYear.getExecutionDegreesSet()) {
			final ExecutionDegree destinationExecutionDegree = getDestinationExecutionDegree(originalExecutionDegree);
			if (destinationExecutionDegree != null) {
				final Degree degree = destinationExecutionDegree.getDegree();
				for (final SchoolClass schoolClass : originalExecutionDegree.getSchoolClassesSet()) {
					SchoolClass newSchoolClass = destinationExecutionDegree.findSchoolClassesByExecutionPeriodAndName(destinationExecutionSemester, schoolClass.getNome());
					if (newSchoolClass == null) {
						final String namePrefix = degree.constructSchoolClassPrefix(schoolClass.getAnoCurricular());
						final String name = schoolClass.getNome().substring(namePrefix.length());
						newSchoolClass = new SchoolClass(destinationExecutionDegree, destinationExecutionSemester, name, schoolClass.getAnoCurricular());
					}
					schoolClassTranslation.put(schoolClass, newSchoolClass);
				}
			}
		}
	}

	private ExecutionDegree getDestinationExecutionDegree(final ExecutionDegree originalExecutionDegree) {
		final ExecutionYear executionYear = destinationExecutionSemester.getExecutionYear();
		final DegreeCurricularPlan degreeCurricularPlan = originalExecutionDegree.getDegreeCurricularPlan();
		for (final ExecutionDegree executionDegree : degreeCurricularPlan.getExecutionDegreesSet()) {
			if (executionYear == executionDegree.getExecutionYear()) {
				return executionDegree;
			}
		}
		return null;
	}

	protected void createFromPreviouse(final ExecutionCourse executionCourse) {
		Row row = spreadsheet.addRow();
		row.setCell("executionCourseId", executionCourse.getExternalId());
		row.setCell("executionCourseAcronym", executionCourse.getSigla());
		row.setCell("curricularCourses", getCurricularCoursesDescription(executionCourse));

		final Set<CurricularCourse> curricularCoursesToAssociate = getActiveCurricularCourses(executionCourse);
		if (!curricularCoursesToAssociate.isEmpty()) {
			row.setCell("create", "Y");
			final ExecutionCourse newExecutionCourse = createExecutionCourse(executionCourse.getNameI18N(), executionCourse.getSigla());
			for (final CurricularCourse curricularCourse : curricularCoursesToAssociate) {
				newExecutionCourse.addAssociatedCurricularCourses(curricularCourse);
			}

			row.setCell("newExecutionCourseId", newExecutionCourse.getExternalId());
			row.setCell("newExecutionCourseAcronym", newExecutionCourse.getSigla());
			row.setCell("newCurricularCourses", getCurricularCoursesDescription(newExecutionCourse));

			processedCurricularCourses.addAll(curricularCoursesToAssociate);
		} else {
			row.setCell("create", "N");
		}
	}

	private String getCurricularCoursesDescription(ExecutionCourse executionCourse) {
		return executionCourse.getAssociatedCurricularCoursesSet().stream()
				.map(cc -> Joiner.on("-").join(cc.getDegreeCurricularPlan().getName(), cc.getAcronym() == null ? cc.getExternalId() : cc.getAcronym())).collect(Collectors.joining(","));
	}

	private void createFor(final CurricularCourse curricularCourse) {
		String acronym = curricularCourse.getAcronym(destinationExecutionSemester);
		if (acronym == null) {
			acronym = curricularCourse.getAcronym();
		}
		if (acronym == null) {
			acronym = curricularCourse.getName().substring(0, 1);
		}

		final ExecutionCourse newExecutionCourse = createExecutionCourse(curricularCourse.getNameI18N(), acronym);
		newExecutionCourse.addAssociatedCurricularCourses(curricularCourse);
		Row row = spreadsheet.addRow();
		row.setCell("executionCourseId", newExecutionCourse.getExternalId());
		row.setCell("executionCourseAcronym", newExecutionCourse.getSigla());
		row.setCell("curricularCourses", getCurricularCoursesDescription(newExecutionCourse));
		row.setCell("create", "Y");
		processedCurricularCourses.add(curricularCourse);
	}

	protected ExecutionCourse createExecutionCourse(final LocalizedString name, final String acronym) {
		final String code = findUniqueCode(acronym);
		ExecutionCourse executionCourse = new ExecutionCourse(name, name.getContent(), code, destinationExecutionSemester, null);
		executionCourse.createEvaluationMethod(evaluationMethod);
		return executionCourse;
	}

	private final Map<String, int[]> codeMap = new HashMap<String, int[]>();

	protected String findUniqueCode(final String acronym) {
		final String key = getKey(acronym);
		int[] count = codeMap.get(key);
		if (count == null) {
			count = new int[] { 1 };
			codeMap.put(key, count);
			return acronym;
		}
		return key + ++count[0];
	}

	protected String getKey(final String acronym) {
		return acronym.indexOf('-') > 0 ? acronym.substring(0, acronym.indexOf('-')) : acronym;
	}

	protected void loadExistingCodes() {
		for (final ExecutionCourse executionCourse : destinationExecutionSemester.getAssociatedExecutionCoursesSet()) {
			findUniqueCode(executionCourse.getSigla());
		}
	}

	protected Set<CurricularCourse> getActiveCurricularCourses(final ExecutionCourse executionCourse) {
		final Set<CurricularCourse> curricularCourses = new HashSet<CurricularCourse>();
		for (final CurricularCourse curricularCourse : executionCourse.getAssociatedCurricularCoursesSet()) {
			if (shouldCreateFor(curricularCourse)) {
				curricularCourses.add(curricularCourse);
			}
		}
		return curricularCourses;
	}

	protected boolean shouldCreateFor(final CurricularCourse curricularCourse) {
		return !processedCurricularCourses.contains(curricularCourse) && isActive(curricularCourse) && !hasExecutionCourse(curricularCourse) && isDegreeTypeToBeProcessed(curricularCourse);
	}

	private boolean isDegreeTypeToBeProcessed(final CurricularCourse curricularCourse) {
		final DegreeType degreeType = curricularCourse.getDegreeType();
		return degreeType.isFirstCycle() || degreeType.isSecondCycle() || degreeType.isIntegratedMasterDegree() || degreeType.getMinor();
	}

	protected boolean hasExecutionCourse(final CurricularCourse curricularCourse) {
		return curricularCourse.getAssociatedExecutionCoursesSet().stream().anyMatch(executionCourse -> destinationExecutionSemester == executionCourse.getExecutionPeriod());
	}

	protected boolean isActive(final CurricularCourse curricularCourse) {
		final DegreeCurricularPlan degreeCurricularPlan = curricularCourse.getDegreeCurricularPlan();
		return curricularCourse.isActive(destinationExecutionSemester) && hasActiveExecutionDegree(degreeCurricularPlan) && hasAprovedCompetenceCourse(curricularCourse);
	}

	private boolean hasAprovedCompetenceCourse(final CurricularCourse curricularCourse) {
		final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
		return competenceCourse == null || competenceCourse.isApproved();
	}

	private boolean hasActiveExecutionDegree(final DegreeCurricularPlan degreeCurricularPlan) {
		final ExecutionYear executionYear = destinationExecutionSemester.getExecutionYear();
		for (final ExecutionDegree executionDegree : degreeCurricularPlan.getExecutionDegreesSet()) {
			if (executionYear == executionDegree.getExecutionYear()) {
				return true;
			}
		}
		return false;
	}

	protected void printExecutionSemesterInfo() {
		taskLog("Initializing next execution semester: %s from %s%n", destinationExecutionSemester.getQualifiedName(), originExecutionSemester.getQualifiedName());
	}

	protected void printReport() {
		taskLog("Processed " + processedCurricularCourses.size() + " curricular courses." + "\n");
		taskLog("Processed " + schoolClassTranslation.size() + " school classes." + "\n");
		Map<DegreeType, Integer> degreeTypeCounter = new HashMap<DegreeType, Integer>();
		Map<Degree, int[]> degreeCounter = new TreeMap<Degree, int[]>(Degree.COMPARATOR_BY_NAME_AND_ID);
		for (final CurricularCourse curricularCourse : processedCurricularCourses) {
			final DegreeCurricularPlan degreeCurricularPlan = curricularCourse.getDegreeCurricularPlan();
			final Degree degree = degreeCurricularPlan.getDegree();
			final DegreeType degreeType = degree.getDegreeType();

			increaseDegreeTypeCount(degreeTypeCounter, degreeType);

			int[] dc = degreeCounter.get(degree);
			if (dc == null) {
				dc = new int[1];
				degreeCounter.put(degree, dc);
			}
			dc[0]++;

			if (!degreeType.isBolonhaType()) {
				taskLog("Non bolonha curricular course: %s - %s\n", degreeCurricularPlan.getName(), curricularCourse.getName());
			}
		}
		for (final DegreeType degreeType : degreeTypeCounter.keySet()) {
			taskLog("   %s: %s\n", degreeType.getName().getContent(), degreeTypeCounter.get(degreeType));

			for (final Entry<Degree, int[]> entry : degreeCounter.entrySet()) {
				final Degree degree = entry.getKey();
				if (degree.getDegreeType() == degreeType) {
					taskLog("      %s: %s\n", degree.getSigla(), Integer.toString(entry.getValue()[0]));
				}
			}
		}

	}

	private void increaseDegreeTypeCount(Map<DegreeType, Integer> degreeTypeCounter, final DegreeType degreeType) {
		Integer counter = degreeTypeCounter.get(degreeType);
		if (counter == null) {
			counter = new Integer(0);
		}
		degreeTypeCounter.put(degreeType, ++counter);
	}
}