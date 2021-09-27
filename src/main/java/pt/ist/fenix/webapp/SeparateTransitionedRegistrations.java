package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.transitions.domain.DegreeCurricularTransitionPlan;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SeparateTransitionedRegistrations extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(r -> r.getLastState() != null && r.getLastState().getStateType() == RegistrationStateType.TRANSITED)
                .filter(this::hasNewSCP)
                .forEach(this::fix);
    }

    private void fix(final Registration registration) {
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
        taskLog("%s\t%s\t%s%n", registration.getNumber(), registration.getDegreeCurricularPlanName(), scp.getExternalId());
        final DegreeCurricularTransitionPlan transitionPlan = scp.getDegreeCurricularPlan().getOriginTransitionPlanSet().iterator().next();

        final Registration originRegistration = registration.getStudent().getRegistrationStream()
                .filter(r -> r != registration)
                .filter(r -> r.getDegree() == transitionPlan.getOriginDegreeCurricularPlan().getDegree())
                .findAny().get();

        final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        final Registration newRegistration = new Registration(registration.getStudent().getPerson(), scp.getDegreeCurricularPlan(),
                originRegistration.getRegistrationProtocol(), scp.getCycleTypes().iterator().next(), currentExecutionYear);
        newRegistration.getLastStudentCurricularPlan().delete();
        newRegistration.setSourceRegistration(originRegistration);
        scp.setRegistration(newRegistration);
        newRegistration.setIngressionType(originRegistration.getIngressionType());
        newRegistration.setRegistrationYear(currentExecutionYear);
        newRegistration.setStartDate(scp.getStartDateYearMonthDay());
        newRegistration.setEntryPhase(originRegistration.getEntryPhase());
        originRegistration.getRegistrationDataByExecutionYearSet().stream()
                .filter(ry -> ry.getExecutionYear() == currentExecutionYear)
                .forEach(ry -> moveToRegistration(ry, newRegistration));
        if (originRegistration.getLastState().getStateType() == RegistrationStateType.REGISTERED) {
            RegistrationState.createRegistrationState(originRegistration,registration.getStudent().getPerson(),
                    scp.getStartDateYearMonthDay().toDateTimeAtMidnight(),RegistrationStateType.TRANSITED);
        }
    }

    private void moveToRegistration(final RegistrationDataByExecutionYear ry, final Registration newRegistration) {
        Method method = null;
        try {
            method = RegistrationDataByExecutionYear.class.getDeclaredMethod("setRegistration", new Class[]{Registration.class,});
            method.setAccessible(true);
            method.invoke(ry, new Object[]{newRegistration});
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }
    }

    private boolean hasNewSCP(final Registration registration) {
        return registration.getLastStudentCurricularPlan().getStartExecutionPeriod().isCurrent();
    }
}
