package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class MoveAttends extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Integer[] numbers = new Integer[] {90741};
        for (Integer studentNumber : numbers) {
            Student student = Student.readStudentByNumber(studentNumber);
            ExecutionSemester currentPeriod = ExecutionSemester.readActualExecutionSemester();

            Registration firstCycleRegistration = student.getRegistrationsSet().stream()
                    .filter(r -> r.getDegree().getCycleTypes().size() == 1)
                    .filter(r -> r.getCurrentCycleType() == CycleType.FIRST_CYCLE).findAny().get();
            Registration secondCycleRegistration = student.getRegistrationsSet().stream()
                    .filter(r -> r.getDegree().getCycleTypes().contains(CycleType.SECOND_CYCLE))
                    .filter(r -> r.getLastState().getStateType().canHaveCurriculumLinesOnCreation())
                    .findAny().get();

            firstCycleRegistration.getAttendsForExecutionPeriod(currentPeriod).forEach(at -> at.setRegistration(secondCycleRegistration));
        }
    }
}
