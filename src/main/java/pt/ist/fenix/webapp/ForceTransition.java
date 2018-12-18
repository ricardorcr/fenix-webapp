package pt.ist.fenix.webapp;

import org.fenixedu.academic.transitions.domain.StudentDegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.service.TransitionService;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ForceTransition extends CustomTask {

    @Override
    public void runTask() throws Exception {
        //1º ciclo vazio - meter
//        StudentDegreeCurricularTransitionPlan studentPlan = FenixFramework.getDomainObject("1979099455165433"); //95281
//        TransitionService.run(studentPlan.getDegreeCurricularTransitionPlan(), studentPlan.getStudent().getPerson().getUser(),
//                false, false, true, true);

        //290249594955214 95278 não está confirmado

        //95276 não tem plano especialização

        //571724571607682 94810

        //Normal
        //1416149501807580 45077
        StudentDegreeCurricularTransitionPlan studentPlan = FenixFramework.getDomainObject("1416149501807580"); //45077
        TransitionService.run(studentPlan.getDegreeCurricularTransitionPlan(), studentPlan.getStudent().getPerson().getUser(),
                false, false, true, true);
    }
}
