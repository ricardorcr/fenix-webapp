package pt.ist.fenix.webapp.task.academic.schedules;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CourseLoad;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EntryPhase;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.FrequencyType;
import org.fenixedu.academic.domain.Holiday;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.space.LessonSpaceOccupation;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.academic.util.HourMinuteSecond;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.Period;
import org.joda.time.TimeOfDay;
import org.joda.time.YearMonthDay;

import com.google.common.base.Joiner;

import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class CopySchedules extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    private int copiedCourseLoads;
    private int notCopiedCourseLoads;
    private int countCopiedLessons;
    private int countInconsistentLessons;
    private int countSkippedLessons;
    private int countSkippedLessons1;
    private int countSkippedLessons2;
    private int countSkippedLessons3;
    private int countSkippedLessons4;

    protected ExecutionSemester originExecutionSemester;
    protected ExecutionSemester destinationExecutionSemester;

    private Spreadsheet spreadsheet;

    protected Map<SchoolClass, SchoolClass> schoolClassTranslation = new HashMap<SchoolClass, SchoolClass>();
    protected Map<OccupationPeriod, Map<Integer, List<LocalDate>>> occupationPeriodDatesMap =
            new HashMap<OccupationPeriod, Map<Integer, List<LocalDate>>>();

    @Override
    public void runTask() throws Exception {

        FenixFramework.atomic(this::create);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        String filename = new DateTime().toString("YYYY_MM_dd_HH_mm") + "_executioncourses_"
                + destinationExecutionSemester.getQualifiedName().replaceAll(" ", "_").replaceAll("/", "_") + ".xls";
        output(filename, baos.toByteArray());

        printReport();
    }

    protected void create() {
        schoolClassTranslation.clear();
        copiedCourseLoads = 0;
        notCopiedCourseLoads = 0;
        countCopiedLessons = 0;
        countInconsistentLessons = 0;
        countSkippedLessons = 0;
        countSkippedLessons1 = 0;
        countSkippedLessons2 = 0;
        countSkippedLessons3 = 0;
        countSkippedLessons4 = 0;

        ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        ExecutionYear nextExecutionYear = currentExecutionYear.getNextExecutionYear();

        originExecutionSemester = currentExecutionYear.getLastExecutionPeriod();
        destinationExecutionSemester = nextExecutionYear.getLastExecutionPeriod();

        spreadsheet = new Spreadsheet("schedule_copy");

        printExecutionSemesterInfo();

        createSchoolClasses();

        loadExistingCodes();

        for (final ExecutionCourse executionCourse : originExecutionSemester.getAssociatedExecutionCoursesSet()) {
            final EntryPhase entryPhase = executionCourse.getEntryPhase();
            if (entryPhase == null || EntryPhase.FIRST_PHASE.equals(entryPhase)) {
                copySchedulesFromPrevious(executionCourse);
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
                    SchoolClass newSchoolClass = destinationExecutionDegree
                            .findSchoolClassesByExecutionPeriodAndName(destinationExecutionSemester, schoolClass.getNome());
                    if (newSchoolClass == null) {
                        final String namePrefix = degree.constructSchoolClassPrefix(schoolClass.getAnoCurricular());
                        final String name = schoolClass.getNome().substring(namePrefix.length());
                        newSchoolClass = new SchoolClass(destinationExecutionDegree, destinationExecutionSemester, name,
                                schoolClass.getAnoCurricular());
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

    protected void copySchedulesFromPrevious(final ExecutionCourse executionCourse) {
        Row row = spreadsheet.addRow();
        row.setCell("executionCourseId", executionCourse.getExternalId());
        row.setCell("executionCourseAcronym", executionCourse.getSigla());
        row.setCell("curricularCourses", getCurricularCoursesDescription(executionCourse));

        ExecutionCourse newExecutionCourse = getDestinationExecutionCourse(executionCourse);
        if (newExecutionCourse != null) {
            row.setCell("create", "Y");

            row.setCell("newExecutionCourseId", newExecutionCourse.getExternalId());
            row.setCell("newExecutionCourseAcronym", newExecutionCourse.getSigla());
            row.setCell("newCurricularCourses", getCurricularCoursesDescription(newExecutionCourse));

            final Map<CourseLoad, CourseLoad> courseLoadMap = new HashMap<CourseLoad, CourseLoad>();
            final Set<Shift> shifts = new HashSet<Shift>();
            for (final CourseLoad oldCourseLoad : executionCourse.getCourseLoadsSet()) {
                final ShiftType shiftType = oldCourseLoad.getType();
                CourseLoad newCourseLoad = newExecutionCourse.getCourseLoadByShiftType(shiftType);
                if (newCourseLoad == null) {
                    newCourseLoad = new CourseLoad(newExecutionCourse, shiftType, oldCourseLoad.getUnitQuantity(),
                            oldCourseLoad.getTotalQuantity());
                }
                if (newCourseLoad != null) {
                    courseLoadMap.put(oldCourseLoad, newCourseLoad);
                    for (final Shift shift : oldCourseLoad.getShiftsSet()) {
                        shifts.add(shift);
                    }
                }
            }

            for (final Shift oldShift : shifts) {
                copyShift(newExecutionCourse, courseLoadMap, oldShift);
            }

            if (executionCourse.getExecutionPeriod().getSemester().intValue() == 2) {
                final ExecutionCourse firstSemesterExecutionCourse = findFirstSemesterExecutionCourse(executionCourse);
                if (firstSemesterExecutionCourse != null) {
                    for (final Professorship professorship : firstSemesterExecutionCourse.getProfessorshipsSet()) {
                        if (professorship.getTeacher()
                                .hasTeacherAuthorization(newExecutionCourse.getExecutionPeriod().getAcademicInterval())) {
                            Professorship.create(professorship.getResponsibleFor(), newExecutionCourse,
                                    professorship.getPerson());
                        }
                    }
                }
            }

        } else {
            row.setCell("create", "N");
        }
    }

    private String getCurricularCoursesDescription(ExecutionCourse executionCourse) {
        return executionCourse.getAssociatedCurricularCoursesSet().stream().map(cc -> Joiner.on("-")
                .join(cc.getDegreeCurricularPlan().getName(), cc.getAcronym() == null ? cc.getExternalId() : cc.getAcronym()))
                .collect(Collectors.joining(","));
    }

    private ExecutionCourse findFirstSemesterExecutionCourse(final ExecutionCourse executionCourse) {
        final ExecutionSemester executionSemester = executionCourse.getExecutionPeriod().getPreviousExecutionPeriod();
        ExecutionCourse result = null;
        for (final CurricularCourse curricularCourse : executionCourse.getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.isAnual()) {
                final ExecutionCourse previous = findPrevious(curricularCourse, executionSemester);
                if (result == null) {
                    result = previous;
                } else if (result != previous) {
                    return null;
                }
            }
        }
        return result;
    }

    private ExecutionCourse findPrevious(CurricularCourse curricularCourse, ExecutionSemester executionSemester) {
        final ExecutionSemester previousExecutionPeriod = executionSemester.getPreviousExecutionPeriod();
        for (final ExecutionCourse executionCourse : curricularCourse.getAssociatedExecutionCoursesSet()) {
            if (executionCourse.getExecutionPeriod() == previousExecutionPeriod) {
                return executionCourse;
            }
        }
        return null;
    }

    private OccupationPeriod findOccupationPeriodFor(final ExecutionSemester executionSemester,
            final CurricularCourse curricularCourse) {
        final ExecutionDegree executionDegree = curricularCourse.getExecutionDegreeFor(executionSemester.getExecutionYear());
        final Set<Integer> curricularYears = curricularCourse.getParentContextsByExecutionSemester(executionSemester).stream()
                .map(context -> context.getCurricularYear()).collect(Collectors.toSet());
        return executionDegree == null ? null : executionDegree.getOccupationPeriodReferencesSet().stream()
                .filter(ref -> ref.getPeriodType() == OccupationPeriodType.LESSONS)
                .filter(ref -> ref.getSemester().intValue() == executionSemester.getSemester().intValue())
                .filter(ref -> overlap(ref.getCurricularYears().getYears(), curricularYears))
                .map(ref -> ref.getOccupationPeriod()).max(Comparator.comparing(OccupationPeriod::getStartDate)).orElse(null);
    }

    private boolean overlap(final Collection<Integer> list, final Set<Integer> set) {
        if (list.isEmpty()) {
            return true;
        }
        for (final Integer year : list) {
            if (year == -1 || set.contains(year)) {
                return true;
            }
        }
        return false;
    }

    private OccupationPeriod findOccupationPeriodFor(final ExecutionCourse executionCourse) {
        final ExecutionSemester semester = executionCourse.getExecutionPeriod();
        return executionCourse.getAssociatedCurricularCoursesSet().stream().map(cc -> findOccupationPeriodFor(semester, cc))
                .filter(Objects::nonNull).distinct().max(Comparator.comparing(OccupationPeriod::getStartDate)).orElse(null);
    }

    private void copyShift(final ExecutionCourse newExecutionCourse, final Map<CourseLoad, CourseLoad> courseLoadMap,
            final Shift oldShift) {
        final Set<ShiftType> shiftTypes = new HashSet<ShiftType>(oldShift.getTypes());
        for (final Iterator<ShiftType> iterator = shiftTypes.iterator(); iterator.hasNext();) {
            final ShiftType shiftType = iterator.next();
            if (!hasCourseLoadForShiftType(courseLoadMap, shiftType)) {
                iterator.remove();
            }
        }
        final Shift newShift = new Shift(newExecutionCourse, shiftTypes, oldShift.getLotacao());

        for (final SchoolClass oldSchoolClass : oldShift.getAssociatedClassesSet()) {
            final SchoolClass newSchoolClass = schoolClassTranslation.get(oldSchoolClass);
            if (newSchoolClass != null) {
                newShift.addAssociatedClasses(newSchoolClass);
            }
        }
        final OccupationPeriod occupationPeriod = findOccupationPeriodFor(newExecutionCourse);

        if (occupationPeriod == null) {
            taskLog("executionCourse withou occ period: %s", newExecutionCourse.getExternalId());
        } else {

            GenericPair<YearMonthDay, YearMonthDay> maxLessonsPeriod = newExecutionCourse.getMaxLessonsPeriod();

            for (final Lesson oldLesson : oldShift.getAssociatedLessonsSet()) {
                YearMonthDay beginDate = occupationPeriod.getStartYearMonthDay();
                YearMonthDay endDate = occupationPeriod.getEndYearMonthDayWithNextPeriods();
                SortedSet<YearMonthDay> instancesDates = getInstancesDates(oldLesson, occupationPeriod);
                if (instancesDates.isEmpty() || oldLesson.getLessonInstancesSet().isEmpty()) {
                    taskLog("\nOldInstances count: %d - newInstances count: %d - %s - %s- %s",
                            oldLesson.getLessonInstancesSet().size(), instancesDates.size(),
                            newExecutionCourse.getDegreePresentationString(), newExecutionCourse.getName(),
                            oldLesson.prettyPrint());
                    return;
                }
                beginDate = instancesDates.first();
                endDate = instancesDates.last();

                try {
                    if (!isConsistent(oldLesson.getDiaSemana(), oldLesson.getInicio(), oldLesson.getFim(), newShift,
                            oldLesson.getFrequency(), destinationExecutionSemester, beginDate, endDate,
                            oldLesson.getLessonCampus(), occupationPeriod, occupationPeriod)) {
                        countSkippedLessons++;
                        continue;
                    }
                } catch (final NullPointerException ex) {
                    taskLog("occupationPeriod: %s ; %s (%s)%n", occupationPeriod, newExecutionCourse.getName(),
                            newExecutionCourse.getDegreePresentationString());
                    throw new Error(ex);
                }
                final Space allocatableSpace = oldLesson.getSala();
                final Space allocatableSpaceToSet;
                if (allocatableSpace != null && allocatableSpace.isFree(generateEventSpaceOccupationIntervals(instancesDates,
                        new HourMinuteSecond(oldLesson.getInicio()), new HourMinuteSecond(oldLesson.getFim())))) {
                    allocatableSpaceToSet = allocatableSpace;
                } else {
                    allocatableSpaceToSet = null;
                }

                try {
                    if (occupationPeriod != null) {

                        LocalTime beginTime = new LocalTime(oldLesson.getInicio());
                        LocalTime endTime = new LocalTime(oldLesson.getFim());
                        DateTime beginLocalDate = new LocalDate(endDate, beginTime.getChronology()).toDateTime(beginTime);
                        DateTime endLocalDate = new LocalDate(endDate, endTime.getChronology()).toDateTime(endTime);

                        Interval interval = new Interval(beginLocalDate, endLocalDate);
                        if (allocatableSpaceToSet != null && !allocatableSpaceToSet.isFree(interval)) {
                            taskLog("\nDomainException: error.LessonSpaceOccupation.room.is.not.free %s - %s - %s - %s - %s",
                                    newExecutionCourse.getName(), newExecutionCourse.getExternalId(),
                                    allocatableSpaceToSet.getName(), beginLocalDate.toString(), endLocalDate.toString());
                            continue;
                        }

                        OccupationPeriod lessonOccupationPeriod = null;
                        if (instancesDates.size() == 1 && beginDate.isEqual(maxLessonsPeriod.getLeft())) {
                            endDate = endDate.plusDays(1);
                        }
                        beginDate = endDate.minusDays(1);
                        try {
                            lessonOccupationPeriod =
                                    OccupationPeriod.createOccupationPeriodForLesson(newExecutionCourse, beginDate, endDate);
                        } catch (DomainException e) {
                            taskLog("\n lessonOccupationPeriod DomainException:  %s\n %s - %s - %s - %s - %s", e.getMessage(),
                                    newExecutionCourse.getName(), newExecutionCourse.getExternalId(),
                                    allocatableSpace == null ? "null" : allocatableSpace.getName(), beginDate.toString(),
                                    endDate.toString());
                        }
                        try {

                            Lesson lesson = new Lesson(oldLesson.getDiaSemana(), oldLesson.getInicio(), oldLesson.getFim(),
                                    newShift, oldLesson.getFrequency(), destinationExecutionSemester, lessonOccupationPeriod,
                                    allocatableSpaceToSet);
                            LessonSpaceOccupation lessonSpaceOccupation = lesson.getLessonSpaceOccupation();
                            lesson.setLessonSpaceOccupation(null);

                            try {
                                final Method method = Lesson.class.getDeclaredMethod("removeLessonSpaceOccupationAndPeriod");
                                method.setAccessible(true);
                                method.invoke(lesson);
                            } catch (Exception e) {
                                throw new Error(e);
                            }
                            lesson.setLessonSpaceOccupation(lessonSpaceOccupation);

                            instancesDates.forEach(date -> {
                                YearMonthDay day = new YearMonthDay(date);
                                if (!Holiday.isHoliday(day, allocatableSpaceToSet)) {
                                    try {
                                        new LessonInstance(lesson, day);
                                    } catch (DomainException e) {
                                        taskLog("\n LessonInstance DomainException:  %s\n %s - %s - %s - %s", e.getMessage(),
                                                newExecutionCourse.getName(), newExecutionCourse.getExternalId(),
                                                allocatableSpaceToSet == null ? "null" : allocatableSpaceToSet.getName(),
                                                date.toString());
                                    }
                                }
                            });
                        } catch (DomainException e) {
                            taskLog("\n Lesson DomainException:  %s\n %s - %s - %s - %s - %s", e.getMessage(),
                                    newExecutionCourse.getName(), newExecutionCourse.getExternalId(),
                                    allocatableSpaceToSet == null ? "null" : allocatableSpaceToSet.getName(),
                                    beginDate.toString(), endDate.toString());
                        }
                    }
                    countCopiedLessons++;
                } catch (DomainException de) {
                    countInconsistentLessons++;
                    throw de;
                }
            }
        }
    }

    private SortedSet<YearMonthDay> getInstancesDates(Lesson oldLesson, OccupationPeriod occupationPeriod) {
        SortedSet<YearMonthDay> result = new TreeSet<YearMonthDay>();
        final OccupationPeriod oldOccupationPeriod = findOccupationPeriodFor(oldLesson.getShift().getExecutionCourse());
        if (oldOccupationPeriod != null) {

            Map<LocalDate, LocalDate> datesShiftMap = getDatesShiftMap(oldOccupationPeriod, occupationPeriod);

            for (LessonInstance lessonInstance : oldLesson.getLessonInstancesSet()) {

                LocalDate newDay = datesShiftMap.get(lessonInstance.getBeginDateTime().toLocalDate());
                if (newDay != null) {
                    result.add(new YearMonthDay(newDay));
                }
//                int weeks = Weeks
//                        .weeksBetween(oldOccupationPeriod.getStartYearMonthDay(), lessonInstance.getBeginDateTime().toLocalDate())
//                        .getWeeks();
//                DateTime start = occupationPeriod.getStartYearMonthDay().toDateTimeAtMidnight().plusWeeks(weeks);
//                YearMonthDay day =
//                        start.withDayOfWeek(oldLesson.getDiaSemana().getDiaSemanaInDayOfWeekJodaFormat()).toYearMonthDay();
//                if (!day.isBefore(start.toYearMonthDay())
//                        && !day.isAfter(occupationPeriod.getLastOccupationPeriodOfNestedPeriods().getEndYearMonthDay())) {
//                    result.add(day);
//                }
            }
        } else {
            taskLog("\noldOccupationPeriod==null: %s", oldLesson.getShift().getExecutionCourse().getDegreePresentationString(),
                    oldLesson.getShift().getExecutionCourse().getName(), oldLesson.getShift().getPresentationName());
        }
        return result;
    }

    private Map<LocalDate, LocalDate> getDatesShiftMap(OccupationPeriod oldOccupationPeriod, OccupationPeriod occupationPeriod) {

        Map<Integer, List<LocalDate>> oldOccupationPeriodMap = getOccupationPeriodDaysByWeekDays(oldOccupationPeriod);
        Map<Integer, List<LocalDate>> newOccupationPeriodMap = getOccupationPeriodDaysByWeekDays(occupationPeriod);

        Map<LocalDate, LocalDate> result = new HashMap<LocalDate, LocalDate>();
        oldOccupationPeriodMap.keySet().forEach(weekDay -> {
            List<LocalDate> oldDays = oldOccupationPeriodMap.get(weekDay);
            List<LocalDate> newDays = newOccupationPeriodMap.get(weekDay);

            for (int i = 0; i < oldDays.size(); i++) {
                result.put(oldDays.get(i), newDays.get(i));
            }
        });

        return result;
    }

    private Map<Integer, List<LocalDate>> getOccupationPeriodDaysByWeekDays(OccupationPeriod occupationPeriod) {
        Map<Integer, List<LocalDate>> occupationPeriodMap = occupationPeriodDatesMap.get(occupationPeriod);
        if (occupationPeriodMap == null) {
            occupationPeriodMap = new HashMap<Integer, List<LocalDate>>();
            OccupationPeriod period = occupationPeriod;

            while (period != null) {
                for (LocalDate day = period.getPeriodInterval().getStart().toLocalDate(); !day
                        .isAfter(period.getPeriodInterval().getEnd().toLocalDate()); day = day.plusDays(1)) {
                    List<LocalDate> days = occupationPeriodMap.get(day.getDayOfWeek());
                    if (days == null) {
                        days = new ArrayList<LocalDate>();
                    }
                    days.add(day);
                    occupationPeriodMap.put(day.getDayOfWeek(), days);
                }
                period = period.getNextPeriod();
            }
            occupationPeriodDatesMap.put(occupationPeriod, occupationPeriodMap);

            taskLog("\n-----------");
            for (Integer weekDay : occupationPeriodMap.keySet()) {
                List<LocalDate> days = occupationPeriodMap.get(weekDay);
                taskLog("{" + weekDay + "} - [" + Stream.of(days).map(d -> d.toString()).collect(Collectors.joining(",")) + "]");

            } ;
            taskLog("\n-----------");

        }
        return occupationPeriodMap;
    }

    private static int SATURDAY_IN_JODA_TIME = 6, SUNDAY_IN_JODA_TIME = 7;

    protected List<Interval> generateEventSpaceOccupationIntervals(SortedSet<YearMonthDay> instancesDates,
            final HourMinuteSecond beginTime, final HourMinuteSecond endTime) {
        List<Interval> result = new ArrayList<Interval>();
        instancesDates.forEach(day -> {
            result.add(createNewInterval(day, day, beginTime, endTime));
        });

        return result;
    }

    protected List<Interval> generateEventSpaceOccupationIntervals(YearMonthDay begin, final YearMonthDay end,
            final HourMinuteSecond beginTime, final HourMinuteSecond endTime, final DiaSemana diaSemana,
            final FrequencyType frequency) {

        final YearMonthDay startDateToSearch = begin;
        final YearMonthDay endDateToSearch = end;

        List<Interval> result = new ArrayList<Interval>();
        begin = getBeginDateInSpecificWeekDay(diaSemana, begin);

        if (frequency == null) {
            if (!begin.isAfter(end)
                    && (startDateToSearch == null || (!end.isBefore(startDateToSearch) && !begin.isAfter(endDateToSearch)))) {
                result.add(createNewInterval(begin, end, beginTime, endTime));
                return result;
            }
        } else {
            int numberOfDaysToSum = frequency.getNumberOfDays();
            while (true) {
                if (begin.isAfter(end)) {
                    break;
                }
                if (startDateToSearch == null || (!begin.isBefore(startDateToSearch) && !begin.isAfter(endDateToSearch))) {

                    Interval interval = createNewInterval(begin, begin, beginTime, endTime);

                    if (!frequency.equals(FrequencyType.DAILY)
                            || ((false || interval.getStart().getDayOfWeek() != SATURDAY_IN_JODA_TIME)
                                    && (false || interval.getStart().getDayOfWeek() != SUNDAY_IN_JODA_TIME))) {

                        result.add(interval);
                    }
                }
                begin = begin.plusDays(numberOfDaysToSum);
            }
        }
        return result;
    }

    protected static Interval createNewInterval(YearMonthDay begin, YearMonthDay end, HourMinuteSecond beginTime,
            HourMinuteSecond endTime) {
        return new Interval(begin.toDateTime(new TimeOfDay(beginTime.getHour(), beginTime.getMinuteOfHour(), 0, 0)),
                end.toDateTime(new TimeOfDay(endTime.getHour(), endTime.getMinuteOfHour(), 0, 0)));
    }

    private YearMonthDay getBeginDateInSpecificWeekDay(DiaSemana diaSemana, YearMonthDay begin) {
        if (diaSemana != null) {
            YearMonthDay newBegin =
                    begin.toDateTimeAtMidnight().withDayOfWeek(diaSemana.getDiaSemanaInDayOfWeekJodaFormat()).toYearMonthDay();
            if (newBegin.isBefore(begin)) {
                begin = newBegin.plusDays(Lesson.NUMBER_OF_DAYS_IN_WEEK);
            } else {
                begin = newBegin;
            }
        }
        return begin;
    }

    public int getFinalNumberOfLessonInstances(final DiaSemana diaSemana, final YearMonthDay start, final YearMonthDay end,
            final Space campus, final OccupationPeriod period, final FrequencyType frequencyType) {
        int count = 0;
        YearMonthDay startDateToSearch = getValidBeginDate(diaSemana, start);
        YearMonthDay endDateToSearch = getValidEndDate(diaSemana, end);
        count += getAllValidLessonDatesWithoutInstancesDates(diaSemana, startDateToSearch, endDateToSearch, campus, period,
                frequencyType).size();
        return count;
    }

    private YearMonthDay getValidBeginDate(final DiaSemana diaSemana, YearMonthDay startDate) {
        YearMonthDay lessonBegin =
                startDate.toDateTimeAtMidnight().withDayOfWeek(diaSemana.getDiaSemanaInDayOfWeekJodaFormat()).toYearMonthDay();
        if (lessonBegin.isBefore(startDate)) {
            lessonBegin = lessonBegin.plusDays(Lesson.NUMBER_OF_DAYS_IN_WEEK);
        }
        return lessonBegin;
    }

    private YearMonthDay getValidEndDate(final DiaSemana diaSemana, YearMonthDay endDate) {
        YearMonthDay lessonEnd =
                endDate.toDateTimeAtMidnight().withDayOfWeek(diaSemana.getDiaSemanaInDayOfWeekJodaFormat()).toYearMonthDay();
        if (lessonEnd.isAfter(endDate)) {
            lessonEnd = lessonEnd.minusDays(Lesson.NUMBER_OF_DAYS_IN_WEEK);
        }
        return lessonEnd;
    }

    private SortedSet<YearMonthDay> getAllValidLessonDatesWithoutInstancesDates(DiaSemana diaSemana,
            YearMonthDay startDateToSearch, YearMonthDay endDateToSearch, final Space campus, final OccupationPeriod period,
            final FrequencyType frequencyType) {

        SortedSet<YearMonthDay> result = new TreeSet<YearMonthDay>();
        startDateToSearch = startDateToSearch != null ? getValidBeginDate(diaSemana, startDateToSearch) : null;

        if (startDateToSearch != null && endDateToSearch != null && !startDateToSearch.isAfter(endDateToSearch)) {

            Space lessonCampus = campus;
            while (true) {
                if (isDayValid(startDateToSearch, lessonCampus, period)) {
                    result.add(startDateToSearch);
                }
                startDateToSearch = startDateToSearch.plusDays(frequencyType.getNumberOfDays());
                if (startDateToSearch.isAfter(endDateToSearch)) {
                    break;
                }
            }
        }

        return result;
    }

    private boolean isDayValid(YearMonthDay day, Space lessonCampus, final OccupationPeriod period) {
        return !Holiday.isHoliday(day.toLocalDate(), lessonCampus) && period.nestedOccupationPeriodsContainsDay(day);
    }

    private boolean isConsistent(final DiaSemana diaSemana, final Calendar inicio, final Calendar fim, final Shift newShift,
            final FrequencyType frequency, final ExecutionSemester executionSemester, final YearMonthDay start,
            final YearMonthDay end, final Space campus, final OccupationPeriod occupationPeriod,
            OccupationPeriod actualOccupationPeriod) {

        final BigDecimal lessonHours =
                BigDecimal.valueOf(Minutes.minutesBetween(new HourMinuteSecond(inicio), new HourMinuteSecond(fim)).getMinutes())
                        .divide(BigDecimal.valueOf(Lesson.NUMBER_OF_MINUTES_IN_HOUR), 2, RoundingMode.HALF_UP);
        final int finalNumberOfLessonInstances = getFinalNumberOfLessonInstances(diaSemana, start, end, campus,
                actualOccupationPeriod == null ? occupationPeriod : actualOccupationPeriod, frequency);
        BigDecimal totalHours =
                newShift.getTotalHours().add(lessonHours.multiply(BigDecimal.valueOf(finalNumberOfLessonInstances)));

        if (newShift.getCourseLoadsSet().size() == 1) {

            final CourseLoad courseLoad = newShift.getCourseLoadsSet().iterator().next();

            if (courseLoad.getUnitQuantity() != null && lessonHours.compareTo(courseLoad.getUnitQuantity()) != 0) {
                countSkippedLessons1++;
                return false;
            }

            if (totalHours.compareTo(courseLoad.getTotalQuantity()) == 1) {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("\n");
                stringBuilder.append(totalHours);
                stringBuilder.append(" : ");
                stringBuilder.append(courseLoad.getTotalQuantity());
                stringBuilder.append(" ... ");
                stringBuilder.append(finalNumberOfLessonInstances);
                stringBuilder.append(" : ");
                stringBuilder.append(lessonHours);
                stringBuilder.append(" : ");
                stringBuilder.append(newShift.getTotalHours());
                stringBuilder.append(" : ");
                stringBuilder.append(newShift.getExecutionCourse().getName());
                stringBuilder.append(" : ");
                stringBuilder.append(newShift.getExecutionCourse().getDegreePresentationString());
                taskLog(stringBuilder.toString() + "\n");
                countSkippedLessons2++;
                //               return false;
            }
        } else {

            boolean unitValid = false, totalValid = false;

            for (CourseLoad courseLoad : newShift.getCourseLoadsSet()) {

                unitValid = false;
                totalValid = false;

                if (courseLoad.getUnitQuantity() == null || lessonHours.compareTo(courseLoad.getUnitQuantity()) == 0) {
                    unitValid = true;
                }
                if (totalHours.compareTo(courseLoad.getTotalQuantity()) != 1) {
                    totalValid = true;
                    if (unitValid) {
                        break;
                    }
                }
            }

            if (!totalValid) {
                countSkippedLessons3++;
                return false;
            }
            if (!unitValid) {
                countSkippedLessons4++;
                return false;
            }

        }

        return true;
    }

    private int findOffset(final Lesson oldLesson) {
        final GenericPair<YearMonthDay, YearMonthDay> maxLessonsPeriod = oldLesson.getExecutionCourse().getMaxLessonsPeriod();
        final LessonInstance lessonInstance = oldLesson.getFirstLessonInstance();
        final Period period;
        if (lessonInstance != null) {
            period = new Period(maxLessonsPeriod.getLeft(), lessonInstance.getDay());
        } else if (oldLesson.getPeriod() != null) {
            final YearMonthDay start = oldLesson.getPeriod().getStartYearMonthDay();
            period = new Period(maxLessonsPeriod.getLeft(), start);
        } else {
            period = null;
        }
        return period == null ? 0 : period.getMonths() * 4 + period.getWeeks() + (period.getDays() / 7);
    }

    private boolean hasCourseLoadForShiftType(final Map<CourseLoad, CourseLoad> courseLoadMap, final ShiftType shiftType) {
        for (final CourseLoad courseLoad : courseLoadMap.values()) {
            if (courseLoad.getType() == shiftType) {
                return true;
            }
        }
        return false;
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

    protected ExecutionCourse getDestinationExecutionCourse(final ExecutionCourse executionCourse) {
        return executionCourse.getAssociatedCurricularCoursesSet().stream()
                .flatMap(cc -> cc.getAssociatedExecutionCoursesSet().stream())
                .filter(ec -> destinationExecutionSemester == ec.getExecutionPeriod()).findAny().orElse(null);
    }

    protected void printExecutionSemesterInfo() {
        taskLog("Initializing next execution semester: %s from %s%n", destinationExecutionSemester.getQualifiedName(),
                originExecutionSemester.getQualifiedName());
    }

    protected void printReport() {
        taskLog("Processed " + schoolClassTranslation.size() + " school classes." + "\n");

        taskLog("Copied " + copiedCourseLoads + " course loads.\n");
        taskLog("Did not copy " + notCopiedCourseLoads + " course loads.\n");
        taskLog("Copied " + countCopiedLessons + " lessons.\n");
        taskLog("Skipped " + countSkippedLessons + " lessons.\n");
        taskLog("Skipped1 " + countSkippedLessons1 + " lessons.\n");
        taskLog("Skipped2 " + countSkippedLessons2 + " lessons. NOTTT SKIPPED\n");
        taskLog("Skipped3 " + countSkippedLessons3 + " lessons.\n");
        taskLog("Skipped4 " + countSkippedLessons4 + " lessons.\n");
        taskLog("Did not copy " + countInconsistentLessons + " inconsistent lessons.\n");
    }

}