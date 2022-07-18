package pt.ist.fenix.webapp.task.academic.schedules;

import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import com.google.common.base.Joiner;

public class MergeExecutionCourses extends CustomTask {

	protected ExecutionSemester destinationExecutionSemester;

	@Override
	public void runTask() throws Exception {

		ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
		ExecutionYear nextExecutionYear = currentExecutionYear.getNextExecutionYear();

		ExecutionSemester originExecutionSemester = currentExecutionYear.getFirstExecutionPeriod();
		destinationExecutionSemester = nextExecutionYear.getFirstExecutionPeriod();

		taskLog("Initializing next execution semester: %s from %s%n", destinationExecutionSemester.getQualifiedName(), originExecutionSemester.getQualifiedName());

		Spreadsheet spreadsheet = new Spreadsheet("schedule_copy");

		for (final ExecutionCourse executionCourse : originExecutionSemester.getAssociatedExecutionCoursesSet()) {
			if (executionCourse.getAssociatedCurricularCoursesSet().size() > 1) {
				ExecutionCourse toExecutionCourse = null;
				for (final CurricularCourse curricularCourse : executionCourse.getAssociatedCurricularCoursesSet()) {
					ExecutionCourse nextExecutionCourse = getNextExecutionCourse(curricularCourse);
					if (nextExecutionCourse == null) {
						taskLog("\nnextExecutionCourse==null curricularCourse= %s - %s", curricularCourse.getExternalId(), curricularCourse.getDegreeCurricularPlan().getDegree().getSigla());
					} else {
						if (toExecutionCourse == null || toExecutionCourse.equals(nextExecutionCourse)) {
							toExecutionCourse = nextExecutionCourse;
						} else {
							Row row = spreadsheet.addRow();
							row.setCell("ID", toExecutionCourse.getExternalId());
							row.setCell("Nome", toExecutionCourse.getName());
							row.setCell("Sigla", toExecutionCourse.getSigla());
							row.setCell("Turnos", toExecutionCourse.getAssociatedShifts().size());
							row.setCell("ID 2", nextExecutionCourse.getExternalId());
							row.setCell("Nome 2", nextExecutionCourse.getName());
							row.setCell("Sigla 2", nextExecutionCourse.getSigla());
							row.setCell("Turnos 2", nextExecutionCourse.getAssociatedShifts().size());
							row.setCell("curricularCourses 1", getCurricularCoursesDescription(toExecutionCourse));
							row.setCell("curricularCourses 2", getCurricularCoursesDescription(nextExecutionCourse));
							if (toExecutionCourse.getAssociatedShifts().size() != 0 && nextExecutionCourse.getAssociatedShifts().size() != 0) {
								row.setCell("MERGED", "N");
							} else {
								org.fenixedu.academic.service.services.manager.MergeExecutionCourses.merge(toExecutionCourse, nextExecutionCourse);
								row.setCell("MERGED", "S");
							}

							row.setCell("curricularCourses Final", getCurricularCoursesDescription(toExecutionCourse));
						}
					}
				}
			}
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		spreadsheet.exportToXLSSheet(baos);
		String filename = new DateTime().toString("YYYY_MM_dd_HH_mm") + "_curricularcourses_" + destinationExecutionSemester.getQualifiedName().replaceAll(" ", "_").replaceAll("/", "_") + ".xls";

		output(filename, baos.toByteArray());

		taskLog("\nDone");
		throw new RuntimeException();
	}

	private String getCurricularCoursesDescription(ExecutionCourse executionCourse) {
		return executionCourse.getAssociatedCurricularCoursesSet().stream().map(cc -> cc.getDegreeCurricularPlan().getName()).collect(Collectors.joining(","));
	}

	protected boolean shouldAssociate(final CurricularCourse curricularCourse) {
		return isActive(curricularCourse) && isDegreeTypeToBeProcessed(curricularCourse);
	}

	private boolean isDegreeTypeToBeProcessed(final CurricularCourse curricularCourse) {
		final DegreeType degreeType = curricularCourse.getDegreeType();
		return degreeType.isFirstCycle() || degreeType.isSecondCycle() || degreeType.isIntegratedMasterDegree() || degreeType.getMinor();
	}

	protected ExecutionCourse getNextExecutionCourse(final CurricularCourse curricularCourse) {
		return curricularCourse.getAssociatedExecutionCoursesSet().stream().filter(executionCourse -> destinationExecutionSemester == executionCourse.getExecutionPeriod()).findFirst().orElse(null);
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
}