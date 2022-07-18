package pt.ist.fenix.webapp.task.academic.schedules.bullet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.fenixedu.academic.domain.CourseLoad;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularYearList;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.FrequencyType;
import org.fenixedu.academic.domain.Holiday;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.OccupationPeriodReference;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.space.LessonSpaceOccupation;
import org.fenixedu.academic.domain.space.SpaceUtils;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.YearMonthDay;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Strings;

import pt.ist.fenixframework.FenixFramework;

public class ImportBulletSchedules extends CustomTask {
	private static final String PL = "PL";
	private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
	private Map<ExecutionCourse, Map<String, Shift>> executionCoursesShiftMap;

	private Map<SchoolClass, Set<String>> bulletSchoolClassesMap;

	@Override
	public void runTask() throws Exception {
		Authenticate.mock(User.findByUsername("ist22986"), "Script");
		ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester().getNextExecutionPeriod();
		taskLog("\nImport schedules for: %s  - EXCEPT TAGUS", executionSemester.getQualifiedName());
		Space tagusparkCampus = FenixFramework.getDomainObject("2448131360898");

		String file = "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/bullet/2022-2023-s1-1c-al.xml";

		final Spreadsheet spreadsheet = new Spreadsheet("Horários");

		executionCoursesShiftMap = new HashMap<ExecutionCourse, Map<String, Shift>>();
		bulletSchoolClassesMap = new HashMap<SchoolClass, Set<String>>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Set<CurricularCourse> ccNotActive = new HashSet<CurricularCourse>();
		try {
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);

			NodeList eventList = doc.getElementsByTagName("Evento");
			for (int nodeIndex = 0; nodeIndex < eventList.getLength(); nodeIndex++) {
				Element element = (Element) eventList.item(nodeIndex);

				String curricularCourseName = element.getElementsByTagName("Nome").item(0).getTextContent();
				String curricularCourseId = element.getElementsByTagName("CodigoDisciplina").item(0).getTextContent();
				CurricularCourse curricularCourse = FenixFramework.getDomainObject(curricularCourseId);
				if (curricularCourse == null) {
					taskLog("\ncurricularCourse null: %s", curricularCourseId);
					continue;
				}
				final ExecutionCourse executionCourse = getExecutionCourse(curricularCourse, executionSemester);
				if (executionCourse == null) {
					taskLog("\nexecutionCourse null: %s", curricularCourseName);
					continue;
				}
				final GenericPair<YearMonthDay, YearMonthDay> maxLessonsPeriod;
				try {
					maxLessonsPeriod = executionCourse.getMaxLessonsPeriod();
				} catch (NullPointerException ex) {
					ccNotActive.add(curricularCourse);
					continue;
				}
				if (maxLessonsPeriod == null) {
					ccNotActive.add(curricularCourse);
					continue;
				}

				List<Space> rooms = getRooms(element, curricularCourseName);
				for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
					Space room = rooms.get(roomIndex);
					if (room != null && tagusparkCampus.equals(SpaceUtils.getSpaceCampus(room))) {
						taskLog("\n\t\t-------- Ignorado evento do Tagus: %s ", curricularCourseName);
					} else {
						Shift shift = getShift(curricularCourseName, element, roomIndex == 0 ? "" : "_" + String.valueOf(roomIndex), executionCourse, curricularCourse, executionSemester);
						if (shift == null) {
							continue;
						}
						OccupationPeriod occupationPeriod = findOccupationPeriodFor(executionSemester, curricularCourse);
						if (occupationPeriod == null) {
							taskLog("\noccupationPeriod == null, %s", curricularCourseId);
						} else {
							String weekDayString = element.getElementsByTagName("DiaDaSemana").item(0).getTextContent();
							String beginHour = element.getElementsByTagName("HoraInicio").item(0).getTextContent();
							String endHour = element.getElementsByTagName("HoraFim").item(0).getTextContent();

							DiaSemana weekDay = getWeekDay(weekDayString);
							Calendar begin = Calendar.getInstance();
							begin.setTime(sdf.parse(beginHour));
							Calendar end = Calendar.getInstance();
							end.setTime(sdf.parse(endHour));

							List<LocalDate> dates = getLessonDates(element);
							YearMonthDay beginDate = new YearMonthDay(dates.get(0));
							if (beginDate.isBefore(maxLessonsPeriod.getLeft())) {
								taskLog("\nDomainException: error.Lesson.invalid.begin.date %s - %s - %s < %s", curricularCourseName, executionCourse.getExternalId(), beginDate.toString(),
										maxLessonsPeriod.getLeft().toString());
								beginDate = maxLessonsPeriod.getLeft();
							}

							LocalDate endDate = dates.get(dates.size() - 1);
							YearMonthDay endYearMonthDay = new YearMonthDay(endDate);

							OccupationPeriod lessonOccupationPeriod = null;
							try {
								if (dates.size() == 01 && beginDate.isEqual(maxLessonsPeriod.getLeft())) {
									endYearMonthDay = endYearMonthDay.plusDays(1);
								}
								beginDate = endYearMonthDay.minusDays(1);

								lessonOccupationPeriod = OccupationPeriod.createOccupationPeriodForLesson(executionCourse, beginDate, endYearMonthDay);
							} catch (DomainException e) {
								taskLog("\n lessonOccupationPeriod DomainException:  %s\n %s - %s - %s - %s - %s", e.getMessage(), curricularCourseName, executionCourse.getExternalId(),
										room == null ? "null" : room.getName(), beginDate.toString(), endYearMonthDay.toString());
							}
							try {
								if (room != null && !room.isFree(new Interval(endDate.toDateTime(new LocalTime(beginHour)), endDate.toDateTime(new LocalTime(endHour))))) {
									taskLog("\nDomainException: error.LessonSpaceOccupation.room.is.not.free %s - %s - %s - %s", curricularCourseName, executionCourse.getExternalId(), room.getName(),
											endDate.toDateTime(new LocalTime(beginHour)).toString(), endDate.toDateTime(new LocalTime(endHour)).toString());
									continue;
								}
								FrequencyType frequencyType = getFrequencyType(dates);
								Lesson lesson = new Lesson(weekDay, begin, end, shift, frequencyType, executionSemester, lessonOccupationPeriod, room);
								LessonSpaceOccupation lessonSpaceOccupation = lesson.getLessonSpaceOccupation();
								lesson.setLessonSpaceOccupation(null);

								final Method method = Lesson.class.getDeclaredMethod("removeLessonSpaceOccupationAndPeriod");
								method.setAccessible(true);
								method.invoke(lesson);
								lesson.setLessonSpaceOccupation(lessonSpaceOccupation);

								dates.forEach(date -> {
									YearMonthDay day = new YearMonthDay(date);
									if (!day.isBefore(maxLessonsPeriod.getLeft())) {
										if (!Holiday.isHoliday(day, room)) {
											try {
												new LessonInstance(lesson, day);
											} catch (DomainException e) {
												taskLog("\n LessonInstance DomainException:  %s\n %s - %s - %s - %s", e.getMessage(), curricularCourseName, executionCourse.getExternalId(),
														room == null ? "null" : room.getName(), date.toString());
											}
										}
									}
								});
								addRow(spreadsheet, curricularCourseName, curricularCourseId, executionCourse, lesson);
							} catch (DomainException e) {
								taskLog("\n Lesson DomainException:  %s\n %s - %s - %s - %s - %s", e.getMessage(), curricularCourseName, executionCourse.getExternalId(),
										room == null ? "null" : room.getName(), beginDate.toString(), endDate.toString());
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		taskLog("\nDone");

		Spreadsheet spreadsheet2 = spreadsheet.addSpreadsheet("Turnos");
		executionCoursesShiftMap.values().forEach(shiftMap -> shiftMap.values().forEach(shift -> {
			Row row = spreadsheet2.addRow();
			row.setCell("Turno OID", shift.getExternalId());
			row.setCell("Turno", shift.getNome());
		}));

		Spreadsheet spreadsheet3 = spreadsheet2.addSpreadsheet("Turmas");
		bulletSchoolClassesMap.keySet().forEach(schoolClasses -> {
			Row row = spreadsheet3.addRow();
			row.setCell("Turma OID", schoolClasses.getExternalId());
			row.setCell("Turma", schoolClasses.getNome());
			row.setCell("Turma bullet", String.join(", ", bulletSchoolClassesMap.get(schoolClasses)));
		});

		ccNotActive.forEach(cc -> {
			taskLog("\nCC not active: %s", cc.getExternalId());
		});

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		spreadsheet.exportToXLSSheet(baos);
		String filename = "schedules_" + new DateTime().toString("dd_MM_YYYY_HH_mm") + ".xls";
		output(filename, baos.toByteArray());
	//	throw new RuntimeException();
	}

	private FrequencyType getFrequencyType(List<LocalDate> dates) {
		if (dates.size() == 1) {
			return FrequencyType.WEEKLY;
		}
		Set<Integer> daysBetween = Stream.iterate(0, n -> n + 1).limit(dates.size() - 1).map(n -> Days.daysBetween(dates.get(n), dates.get(n + 1)).getDays()).distinct().collect(Collectors.toSet());

		if (daysBetween.size() == 1) {
			Integer days = daysBetween.iterator().next();
			if (days == FrequencyType.DAILY.getNumberOfDays()) {
				return FrequencyType.DAILY;
			} else if (days == FrequencyType.WEEKLY.getNumberOfDays()) {
				return FrequencyType.WEEKLY;
			} else if (days == FrequencyType.BIWEEKLY.getNumberOfDays()) {
				return FrequencyType.BIWEEKLY;
			}
		}
		return FrequencyType.WEEKLY;
	}

	private static YearMonthDay[] createNewPeriodWithExclusions(final YearMonthDay beginDate, final YearMonthDay endDate, final OccupationPeriod result) {
		final SortedSet<YearMonthDay> dates = new TreeSet<YearMonthDay>();

		dates.add(beginDate);
		dates.add(endDate);

		OccupationPeriod pop = result;
		for (OccupationPeriod nop = result.getNextPeriod(); nop != null; pop = nop, nop = nop.getNextPeriod()) {
			if (!pop.getEndYearMonthDay().isBefore(beginDate) && pop.getEndYearMonthDay().isBefore(endDate)) {
				if (pop.getEndYearMonthDay().equals(beginDate)) {
					dates.add(pop.getEndYearMonthDay().minusDays(1));
				} else {
					dates.add(pop.getEndYearMonthDay());
				}
			}
			if (nop.getStartYearMonthDay().isAfter(beginDate) && !nop.getStartYearMonthDay().isAfter(endDate)) {
				dates.add(nop.getStartYearMonthDay());
			}
		}

		return dates.toArray(new YearMonthDay[0]);
	}

	private List<LocalDate> getLessonDates(Element element) {
		List<LocalDate> dates = new ArrayList<LocalDate>();
		Element lessonElement = (Element) element.getElementsByTagName("DatasDoDia").item(0);
		NodeList lessonDatesElementList = lessonElement.getElementsByTagName("Data");
		for (int lessontNodeIndex = 0; lessontNodeIndex < lessonDatesElementList.getLength(); lessontNodeIndex++) {
			Element dateElement = (Element) lessonDatesElementList.item(lessontNodeIndex);
			dates.add(LocalDate.parse(dateElement.getTextContent()));
		}
		return dates;
	}

	private Shift getShift(String curricularCourseName, Element element, String roomIndex, ExecutionCourse executionCourse, CurricularCourse curricularCourse, ExecutionSemester executionSemester) {
		String shiftTypeString = element.getElementsByTagName("Tipologia").item(0).getTextContent();
		ShiftType shiftType = getShiftType(shiftTypeString);
		if (shiftType == null) {
			return null;
		}
		NodeList shiftElementList = element.getElementsByTagName("Turno");
		if (shiftElementList.getLength() > 1) {
			taskLog("Event with more than 1 shift???: " + curricularCourseName);
		}
		Shift shift = null;
		Element shiftElement = (Element) shiftElementList.item(0);
		String shiftName = shiftElement.getElementsByTagName("Nome").item(0).getTextContent();
		
        if (shiftTypeString.matches("[a-zA-Z][0-9]")) {
            taskLog("\n old shift name: %s", shiftName);
            shiftName = shiftName.replaceAll(shiftTypeString, shiftTypeString.replaceAll("[0-9]", ""));
            taskLog(" new shift name: %s", shiftName);
        }
		
		
		Integer lotacao = Integer.valueOf(shiftElement.getElementsByTagName("NumAlunos").item(0).getTextContent());
		if (Strings.isNullOrEmpty(shiftName)) {// TODO
		}

		shift = getShift(executionCourse, shiftType, lotacao, shiftName.concat(roomIndex));

		NodeList schoolClassElementList = element.getElementsByTagName("Turma");
		for (int schoolClassIndex = 0; schoolClassIndex < schoolClassElementList.getLength(); schoolClassIndex++) {
			Element schoolClassElement = (Element) schoolClassElementList.item(schoolClassIndex);
			SchoolClass schoolClass = getSchoolClass(schoolClassElement, executionSemester);
			if (schoolClass == null) {
				taskLog(" %s", curricularCourseName);
			} else {
				shift.addAssociatedClasses(schoolClass);
			}
		}
		return shift;
	}

	private List<Space> getRooms(Element element, String curricularCourseName) {
		Set<Space> spaces = new HashSet<Space>();
		NodeList roomElementList = element.getElementsByTagName("Sala");
		Space room = null;
		for (int roomNodeIndex = 0; roomNodeIndex < roomElementList.getLength(); roomNodeIndex++) {
			if (roomNodeIndex == 1) {
				taskLog("\nDuplicate shift: %s", curricularCourseName);
			}
			Element roomElement = (Element) roomElementList.item(roomNodeIndex);
			String roomName = roomElement.getElementsByTagName("Nome").item(0).getTextContent();
			room = getSpaceByName(roomName);
			if (room == null) {
				taskLog("\nInvalid room: %s", roomName);
			} else {
				spaces.add(room);
				// taskLog("\nroomName: %s", roomName);
			}
		}
		if (roomElementList.getLength() == 0) {
			spaces.add(room);
			// taskLog("\nNO ROOM: %s", curricularCourseName);
		}
		return new ArrayList<Space>(spaces);
	}

	public Space getSpaceByName(String name) {
		String name2 = getName(name);
		 return Space.getSpaces().filter(space -> name2.equals(space.getName().trim()) && SpaceUtils.isForEducation(space)).findFirst().orElse(null);
	}

	private String getName(String name) {
		switch (name) {
		case "LAB 13":
			return "LAB 1";
		case "LAB 14":
			return "LAB 2";
		case "LAB 10":
			return "LAB 3";
		case "LAB 8":
			return "LAB 4";
		case "LAB 11":
			return "LAB 5";
		case "Laboratório Mecanica Fluidos":
			return "Laboratório Mecanica Fluidos; Laboratório Motores Térmicos";
		case "1.1.1a LM / 1.1.1b LIC":
		case "1.1.1a  LM  / 1.1.1b LIC":
		case "1.1.1a  LM  / 1.1.1b LIC":
			return "1.1.1a  LM  ";
		case "LTI sala 5 - Q5.2/Ensino Informático":
			return "LTI SALA 5 - Q5.2";
		case "Lab Tecnologia Mecânica":
			return "Lab.Tecnologia Mecânica";
		case "V01.09":
			return "V01.09 - Lab. Didático do LERM";
		case "1.4 LMT":
			return "1.4 Laboratório dos Grupos TLMOTO e PSEM";
		default:
			break;
		}

		return name;
	}

	private void addRow(final Spreadsheet spreadsheet, String curricularCourseName, String curricularCourseId, ExecutionCourse executionCourse, Lesson lesson) {
		Row row = spreadsheet.addRow();
		row.setCell("Bullet curricularCourse name", curricularCourseName);
		row.setCell("Bullet curricularCourseId", curricularCourseId);
		row.setCell("executionCourseId", executionCourse == null ? "-" : executionCourse.getExternalId());
		row.setCell("executionCourseName", executionCourse == null ? "-" : executionCourse.getName());

		if (lesson != null) {
			row.setCell("Turno", lesson.getShift().getExternalId());
			row.setCell("Turno Bullet", lesson.getShift().getComment());
			row.setCell("Turmas", lesson.getShift().getClassesPrettyPrint());
			row.setCell("Aula", lesson.prettyPrint());
			row.setCell("Instâncias", String.join(", ", lesson.getAllLessonInstanceDates().stream().map(date -> date.toString()).collect(Collectors.toSet())));
		}
	}

	private void createLessonPeriods(ExecutionSemester executionSemester, Iterable<Interval> intervals, CurricularYearList curricularYearList, Set<ExecutionDegree> executiondegrees) {
		OccupationPeriod occupationPeriod = new OccupationPeriod(intervals.iterator());
		executiondegrees
				.forEach(executiondegree -> new OccupationPeriodReference(occupationPeriod, executiondegree, OccupationPeriodType.LESSONS, executionSemester.getSemester(), curricularYearList));
	}

	private DiaSemana getWeekDay(String weekDayString) {
		switch (weekDayString) {
		case "Segunda":
			return new DiaSemana(DiaSemana.SEGUNDA_FEIRA);
		case "Terça":
			return new DiaSemana(DiaSemana.TERCA_FEIRA);
		case "Quarta":
			return new DiaSemana(DiaSemana.QUARTA_FEIRA);
		case "Quinta":
			return new DiaSemana(DiaSemana.QUINTA_FEIRA);
		case "Sexta":
			return new DiaSemana(DiaSemana.SEXTA_FEIRA);
		case "Sábado":
			return new DiaSemana(DiaSemana.SABADO);
		case "Domingo":
			return new DiaSemana(DiaSemana.DOMINGO);
		}
		return null;
	}

	private OccupationPeriod findOccupationPeriodFor(final ExecutionSemester executionSemester, final CurricularCourse curricularCourse) {
		final ExecutionDegree executionDegree = curricularCourse.getExecutionDegreeFor(executionSemester.getExecutionYear());
		final Set<Integer> curricularYears = curricularCourse.getParentContextsByExecutionSemester(executionSemester).stream().map(context -> context.getCurricularYear()).collect(Collectors.toSet());
		return executionDegree.getOccupationPeriodReferencesSet().stream().filter(ref -> ref.getPeriodType() == OccupationPeriodType.LESSONS)
				.filter(ref -> ref.getSemester().intValue() == executionSemester.getSemester().intValue()).filter(ref -> overlap(ref.getCurricularYears().getYears(), curricularYears))
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

	private Shift getShift(ExecutionCourse executionCourse, ShiftType shiftType, Integer lotacao, String shiftName) {
		CourseLoad courseLoad = executionCourse.getCourseLoadByShiftType(shiftType);
		if (courseLoad == null) {
			new CourseLoad(executionCourse, shiftType, null, BigDecimal.valueOf(100));// TODO
		}
		Map<String, Shift> shiftMap = executionCoursesShiftMap.get(executionCourse);
		if (shiftMap == null) {
			shiftMap = new HashMap<String, Shift>();
			executionCoursesShiftMap.put(executionCourse, shiftMap);
		}
		Shift shift = shiftMap.get(shiftName);
		if (shift == null) {
			List<ShiftType> types = new ArrayList<ShiftType>();
			types.add(shiftType);
			shift = new Shift(executionCourse, types, lotacao);
			shift.setComment(shiftName);
			shiftMap.put(shiftName, shift);
		} else {
			List<ShiftType> types = shift.getTypes();
			if (!types.contains(shiftType)) {
				types.add(shiftType);
			}
			shift.edit((List<ShiftType>) types, lotacao, executionCourse, shift.getNome(), shiftName);
		}
		return shift;
	}

	private ExecutionCourse getExecutionCourse(CurricularCourse curricularCourse, ExecutionSemester executionSemester) {
		List<ExecutionCourse> executionCoursesByExecutionPeriod = curricularCourse.getExecutionCoursesByExecutionPeriod(executionSemester);
		if (executionCoursesByExecutionPeriod.isEmpty()) {
			return null;
		} else if (executionCoursesByExecutionPeriod.size() == 1) {
			return executionCoursesByExecutionPeriod.iterator().next();
		}
		taskLog("\n+ que um executioncourse. cc: " + curricularCourse.getOid());
		return null;
	}

	private SchoolClass getSchoolClass(Element schoolClassElement, ExecutionSemester executionSemester) {
		String bulletSchoolClassName = schoolClassElement.getElementsByTagName("NomeTurma").item(0).getTextContent();
		Integer curricularYear = Integer.valueOf(schoolClassElement.getElementsByTagName("Ano").item(0).getTextContent());
		String degreeSigla = schoolClassElement.getElementsByTagName("SiglaCurso").item(0).getTextContent();
		String dcpName = schoolClassElement.getElementsByTagName("CodigoPlanoCurricular").item(0).getTextContent();
		int indexOf = dcpName.indexOf("#");
		dcpName = dcpName.substring(0, indexOf != -1 ? indexOf : dcpName.indexOf("/"));
		DegreeCurricularPlan dcp = DegreeCurricularPlan.readByNameAndDegreeSigla(dcpName, degreeSigla);
		ExecutionDegree executionDegree = dcp.getExecutionDegreeByYear(executionSemester.getExecutionYear());
		if (executionDegree == null) {
			taskLog("\nexecutionDegree null %s, %s, %s", dcpName, degreeSigla, bulletSchoolClassName);
			return null;
		}
		String schoolClassName = bulletSchoolClassName.substring(bulletSchoolClassName.indexOf(String.valueOf(curricularYear)) + 1);
		SchoolClass schoolClass = getSchoolClassByName(executionDegree, executionSemester, curricularYear, schoolClassName);

		if (schoolClass == null) {
			schoolClass = new SchoolClass(executionDegree, executionSemester, schoolClassName, curricularYear);
		}

		Set<String> bulletSchoolClassSet = bulletSchoolClassesMap.get(schoolClass);
		if (bulletSchoolClassSet == null) {
			bulletSchoolClassSet = new HashSet<String>();
		}
		bulletSchoolClassSet.add(bulletSchoolClassName);
		bulletSchoolClassesMap.put(schoolClass, bulletSchoolClassSet);

		return schoolClass;
	}

	private SchoolClass getSchoolClassByName(ExecutionDegree executionDegree, ExecutionSemester executionSemester, Integer curricularYear, String className) {
		final DegreeCurricularPlan degreeCurricularPlan = executionDegree.getDegreeCurricularPlan();
		final Set<SchoolClass> classes = executionDegree.findSchoolClassesByExecutionPeriodAndCurricularYear(executionSemester, curricularYear);
		final Degree degree = degreeCurricularPlan.getDegree();
		final String constructedSchoolClassName = degree.constructSchoolClassPrefix(curricularYear) + className;

		for (final SchoolClass schoolClass : classes) {
			if (constructedSchoolClassName.equalsIgnoreCase(schoolClass.getNome())) {
				return schoolClass;
			}
		}
		return null;
	}

	private ShiftType getShiftType(String shiftTypeString) {
		shiftTypeString = shiftTypeString.replaceAll("[0-9]", "");
		if (!Strings.isNullOrEmpty(shiftTypeString)) {
			if (shiftTypeString.equalsIgnoreCase(ShiftType.TEORICA.getSiglaTipoAula())) {
				return ShiftType.TEORICA;
			}
			if (shiftTypeString.equalsIgnoreCase(ShiftType.PRATICA.getSiglaTipoAula())) {
				return ShiftType.PRATICA;
			}
			if (shiftTypeString.equalsIgnoreCase(ShiftType.TEORICO_PRATICA.getSiglaTipoAula())) {
				return ShiftType.TEORICO_PRATICA;
			}
			if (shiftTypeString.equalsIgnoreCase(ShiftType.LABORATORIAL.getSiglaTipoAula()) || shiftTypeString.equalsIgnoreCase(PL)) {
				return ShiftType.LABORATORIAL;
			}
			if (shiftTypeString.equalsIgnoreCase(ShiftType.SEMINARY.getSiglaTipoAula())) {
				return ShiftType.SEMINARY;
			}
			if (shiftTypeString.equalsIgnoreCase(ShiftType.PROBLEMS.getSiglaTipoAula())) {
				return ShiftType.PROBLEMS;
			}
			if (shiftTypeString.equalsIgnoreCase(ShiftType.FIELD_WORK.getSiglaTipoAula())) {
				return ShiftType.FIELD_WORK;
			}
			if (shiftTypeString.equalsIgnoreCase(ShiftType.TRAINING_PERIOD.getSiglaTipoAula())) {
				return ShiftType.TRAINING_PERIOD;
			}
			if (shiftTypeString.equalsIgnoreCase(ShiftType.TUTORIAL_ORIENTATION.getSiglaTipoAula())) {
				return ShiftType.TUTORIAL_ORIENTATION;
			}
		}
		taskLog("Invalid shiftType: " + shiftTypeString);
		return null;
	}

}