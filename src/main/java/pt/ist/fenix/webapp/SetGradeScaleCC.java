package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class SetGradeScaleCC extends CustomTask {

    @Override
    public void runTask() throws Exception {
        CurricularCourse thesisProposal = FenixFramework.getDomainObject("1529008490553"); //CC Proposta de Tese
        thesisProposal.setGradeScale(GradeScale.TYPEAP);

        CurricularCourse portfolio = FenixFramework.getDomainObject("1408903891910679"); //CC Portfólio em Inovação Interdisciplinar
        portfolio.setGradeScale(GradeScale.TYPEAP);
    }
}
