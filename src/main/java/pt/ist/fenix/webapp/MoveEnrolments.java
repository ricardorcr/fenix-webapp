package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.quc.domain.StudentInquiryRegistry;
import pt.ist.fenixframework.FenixFramework;

public class MoveEnrolments extends CustomTask {

	@Override
	public void runTask() throws Exception {
		// Student student = Student.readStudentByNumber(83394);
		ExecutionSemester currentPeriod = ExecutionSemester.readActualExecutionSemester();

		Registration firstCycleRegistration = FenixFramework.getDomainObject("846684082932098");
		// student.getRegistrationsSet().stream().filter(r -> r.getCurrentCycleType() ==
		// CycleType.FIRST_CYCLE).findAny().get();
		Registration secondCycleRegistration = FenixFramework.getDomainObject("1691109013090641");
		// student.getRegistrationsSet().stream().filter(r ->
		// r.getDegree().getCycleTypes().contains(CycleType.SECOND_CYCLE)).findAny().get();

		StudentCurricularPlan secondCycleRegistrationSCP = secondCycleRegistration.getStudentCurricularPlan(currentPeriod.getExecutionYear());		
		firstCycleRegistration.getEnrolments(currentPeriod).stream()
				.peek(e -> taskLog("Enrolment: %s Antes tinha o scp: %s e vou passar a ter: %s%n", e.getExternalId(),
						e.getStudentCurricularPlan().getExternalId(), secondCycleRegistrationSCP.getExternalId()))
				.forEach(e -> {
					CurriculumGroup curriculumGroup = 
						secondCycleRegistrationSCP.getRoot().getAllCurriculumGroups().stream()
							.filter(cg -> cg.getDegreeModule() != null)
							.filter(cg -> cg.getDegreeModule() == e.getCurriculumGroup().getDegreeModule())
							.findAny().get();
					e.setCurriculumGroup(curriculumGroup);
					e.setStudentCurricularPlan(secondCycleRegistrationSCP);
				});

		firstCycleRegistration.getAttendsForExecutionPeriod(currentPeriod).forEach(at -> at.setRegistration(secondCycleRegistration));
		
		//remove StudentInquiryRegistration for first cycle, it no longer makes sense
//		StudentInquiryRegistry registry = FenixFramework.getDomainObject("850343395141094");
//		registry.delete();
	}
}