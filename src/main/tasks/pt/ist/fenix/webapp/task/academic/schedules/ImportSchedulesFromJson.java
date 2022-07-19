package pt.ist.fenix.webapp.task.academic.schedules;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.FrequencyType;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.candidacy.degree.ShiftDistribution;
import org.fenixedu.academic.domain.candidacy.degree.ShiftDistributionEntry;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.space.LessonSpaceOccupation;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.YearMonthDay;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kong.unirest.Unirest;
import pt.ist.fenixframework.FenixFramework;

public class ImportSchedulesFromJson extends CustomTask {

	@SuppressWarnings("deprecation")
	@Override
	public void runTask() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		DateTimeFormatter dateFormat = DateTimeFormat.forPattern("YYYY/MM/dd");

		String json = Unirest.get("https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/bullet/deletedSchedules22_23_1s.json").asString().getBody();
		final JsonObject data = new JsonParser().parse(json).getAsJsonObject();

		JsonArray shifts = data.getAsJsonArray("shifts");

		for (JsonElement shiftElement : shifts.getAsJsonArray()) {
			JsonObject shiftJson = shiftElement.getAsJsonObject();
			Shift shift = getShift(shiftJson);

			for (JsonElement lessonElementJson : shiftJson.get("lessons").getAsJsonArray()) {
				JsonObject lessonJson = lessonElementJson.getAsJsonObject();
				DiaSemana weekDay = new DiaSemana(lessonJson.get("weekDay").getAsInt());
				Calendar begin = Calendar.getInstance();
				Calendar end = Calendar.getInstance();
				begin.setTime(sdf.parse(lessonJson.get("begin").getAsString()));
				end.setTime(sdf.parse(lessonJson.get("end").getAsString()));

				FrequencyType frequencyType = FrequencyType.valueOf(lessonJson.get("frequencyType").getAsString());
				ExecutionSemester executionSemester = FenixFramework.getDomainObject(lessonJson.get("executionSemester").getAsString());

				JsonArray instancesArray = lessonJson.get("instances").getAsJsonArray();

				ExecutionCourse executionCourse = shift.getExecutionCourse();
				Space room = FenixFramework.getDomainObject(lessonJson.get("room").getAsString());

				JsonArray occupationPeriods = lessonJson.get("occupationPeriods").getAsJsonArray();

				OccupationPeriod lessonOccupationPeriod = null;
				OccupationPeriod previous = null;
				for (JsonElement periodElement : occupationPeriods) {
					JsonObject periodJson = periodElement.getAsJsonObject();

					YearMonthDay beginYearMonthDay = new YearMonthDay(dateFormat.parseLocalDate(periodJson.get("beginYearMonthDay").getAsString()));
					YearMonthDay endYearMonthDay = new YearMonthDay(dateFormat.parseLocalDate(periodJson.get("endYearMonthDay").getAsString()));
					OccupationPeriod occupationPeriod = new OccupationPeriod(beginYearMonthDay, endYearMonthDay);

					if (previous == null) {
						lessonOccupationPeriod = occupationPeriod;
					} else {
						previous.setNextPeriod(occupationPeriod);
					}
					previous = occupationPeriod;
				}

				final GenericPair<YearMonthDay, YearMonthDay> maxLessonsPeriod = executionCourse.getMaxLessonsPeriod();
				if (occupationPeriods.size() == 0) {
					if (instancesArray.size() == 0) {
						taskLog("\nERRO: " + lessonJson.toString());
						continue;
					}
					YearMonthDay endYearMonthDay = new YearMonthDay(dateFormat.parseLocalDate(instancesArray.get(0).getAsString()));
					YearMonthDay beginYearMonthDay = new YearMonthDay(dateFormat.parseLocalDate(instancesArray.get(instancesArray.size() - 1).getAsString()));

					if (instancesArray.size() == 01 && beginYearMonthDay.isEqual(maxLessonsPeriod.getLeft())) {
						endYearMonthDay = endYearMonthDay.plusDays(1);
					}
					beginYearMonthDay = endYearMonthDay.minusDays(1);

					lessonOccupationPeriod = OccupationPeriod.createOccupationPeriodForLesson(executionCourse, beginYearMonthDay, endYearMonthDay);
				}

			//	taskLog("begin %s - end %s - maxLessonsPeriod: %s", lessonOccupationPeriod.getStart().toString(), lessonOccupationPeriod.getEnd().toString(), maxLessonsPeriod.getLeft());
				try {
					Lesson lesson = new Lesson(weekDay, begin, end, shift, frequencyType, executionSemester, lessonOccupationPeriod, room);

					if (occupationPeriods.size() == 0) {
						LessonSpaceOccupation lessonSpaceOccupation = lesson.getLessonSpaceOccupation();
						lesson.setLessonSpaceOccupation(null);

						final Method method = Lesson.class.getDeclaredMethod("removeLessonSpaceOccupationAndPeriod");
						method.setAccessible(true);
						method.invoke(lesson);
						lesson.setLessonSpaceOccupation(lessonSpaceOccupation);
					}
					instancesArray.forEach(day -> {

						try {
							new LessonInstance(lesson, new YearMonthDay(dateFormat.parseLocalDate(day.getAsString())));
						} catch (DomainException e) {
							taskLog("\n LessonInstance DomainException:  %s\n %s - %s - %s - %s", e.getMessage(), shift.getPresentationName(), executionCourse.getExternalId(),
									room == null ? "null" : room.getName(), day.getAsString());
						}

					});
				} catch (DomainException e) {
					taskLog("\n Lesson DomainException:  %s\n %s - %s - %s - %s - %s", e.getMessage(), shift.getPresentationName(), executionCourse.getExternalId(),
							room == null ? "null" : room.getName(), lessonOccupationPeriod.getStart().toString(), lessonOccupationPeriod.getEnd().toString());
					continue;
				}

			}

		}

		taskLog("\nDone! %s shifts", shifts.size());
		throw new RuntimeException();
	}

	private Shift getShift(JsonObject shiftJson) {
		String asString = shiftJson.get("externalId").getAsString();
		Shift shift = FenixFramework.getDomainObject(asString);
		//taskLog("\n%s", asString);
		if (!FenixFramework.isDomainObjectValid(shift)) {
			List<ShiftType> types = new ArrayList<ShiftType>();
			shiftJson.get("shiftTypes").getAsJsonArray().forEach(st -> types.add(ShiftType.valueOf(st.getAsString())));
			ExecutionCourse executionCourse = FenixFramework.getDomainObject(shiftJson.get("executionCourse").getAsString());
			shift = new Shift(executionCourse, types, shiftJson.get("lotacao").getAsInt());
			JsonElement jsonElement = shiftJson.get("comment");
			if (!jsonElement.isJsonNull()) {
				shift.setComment(jsonElement.getAsString());
			}
			for (JsonElement shiftProfessorshipsElement : shiftJson.get("shiftProfessorships").getAsJsonArray()) {
				final JsonObject shiftProfessorshipJson = shiftProfessorshipsElement.getAsJsonObject();
				Person person = FenixFramework.getDomainObject(shiftProfessorshipJson.get("person").getAsString());
				Professorship.create(shiftProfessorshipJson.get("responsibleFor").getAsBoolean(), executionCourse, person);
			}

			for (JsonElement shiftDistributionEntryElement : shiftJson.get("shiftDistributionEntries").getAsJsonArray()) {
				final JsonObject shiftDistributionEntryJson = shiftDistributionEntryElement.getAsJsonObject();
				ShiftDistribution shiftDistribution = FenixFramework.getDomainObject(shiftDistributionEntryJson.get("shiftDistribution").getAsString());
				ExecutionDegree executionDegree = FenixFramework.getDomainObject(shiftDistributionEntryJson.get("executionDegree").getAsString());
				new ShiftDistributionEntry(shiftDistribution, executionDegree, shift, shiftDistributionEntryJson.get("abstractStudentNumber").getAsInt());
			}

			for (JsonElement schoolClassId : shiftJson.get("schoolClasses").getAsJsonArray()) {
				SchoolClass schoolClass = FenixFramework.getDomainObject(schoolClassId.getAsString());
				shift.addAssociatedClasses(schoolClass);
			}
		}
		return shift;
	}

}