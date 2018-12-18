package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.integration.domain.student.PreEnrolment;
import pt.ist.fenixframework.FenixFramework;

public class InitializePreEnrolments extends CustomTask {

    //courses
    //"283003985068104",
    //"name" : "Administração e Gestão de Infraestruturas de It"

    //groups
    //"id" : "281882998604003",
    //"name" : "Sistemas Empresariais",

    //"id" : "281882998604004",
    //"name" : "Tecnologia dos Sistemas Informáticos",
    @Override
    public void runTask() throws Exception {
        Registration registration = FenixFramework.getDomainObject("846684082930899");
        ExecutionSemester executionSemester = FenixFramework.getDomainObject("566806834053123");

        try {
        new PreEnrolment(registration.getPerson().getUser(), FenixFramework.getDomainObject("283003985068104"),
                FenixFramework.getDomainObject("281882998604003"), registration.getDegree(), executionSemester);
        } catch (Exception e) {
            taskLog(e.getMessage());
        }
        try {
        new PreEnrolment(registration.getPerson().getUser(), FenixFramework.getDomainObject("283003985068104"),
                FenixFramework.getDomainObject("281882998604004"), registration.getDegree(), executionSemester);
        } catch (Exception e) {
            taskLog("2 - " + e.getMessage());
        }

//        Set<CurricularCourse> result = new HashSet<CurricularCourse>();
//        actualExecutionSemester.getPreEnrolmentsSet().stream().forEach(pe -> {
//            pe.getCurricularCourse().getParentContextsByExecutionSemester(actualExecutionSemester).stream()
//                    .filter(ctx -> ctx.getCurricularPeriod().getChildOrder() == 2)
//                    .forEach(ctx -> result.add(pe.getCurricularCourse()));
//        });
//        result.stream().forEach(cc -> taskLog("Esta é do 2º semestre! %s\n", cc.getExternalId()));
        
        
//        ExecutionSemester actualExecutionSemester = ExecutionSemester.readActualExecutionSemester();
//        actualExecutionSemester.getPreEnrolmentsSet().stream().filter(pe -> pe.getCourseGroup().getDegree() != pe.getDegree())
//                .forEach(pe -> taskLog("Grupo\t%s\t%s\tDisciplina\t%s\t%s\tUser%s\n", pe.getCourseGroup().getExternalId(),
//                        pe.getCourseGroup().getName(), pe.getCurricularCourse().getExternalId(),
//                        pe.getCurricularCourse().getName(), pe.getUser().getUsername()));

    }
}
