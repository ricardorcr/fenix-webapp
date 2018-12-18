package pt.ist.fenix.webapp;

import java.util.Set;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularCourseEquivalence;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.FenixFramework;

public class RemoveCurricularCourseEquivalence extends CustomTask {
    
    @Override
    public void runTask() throws Exception {

        CurricularCourse curricularCourse = FenixFramework.getDomainObject("1529008486932");
        Set<CurricularCourseEquivalence> curricularCourseEquivalences = curricularCourse.getCurricularCourseEquivalencesSet();
        for (CurricularCourseEquivalence equiv : curricularCourseEquivalences) {
            if (equiv.getExternalId().equals("1103806765830")) {
                taskLog("%s %s EquivalentCurricularCourse to remove %s%n", curricularCourse.getExternalId(), curricularCourse.getAcronym(), equiv.getExternalId());
                equiv.delete();
            }
        }                        
        taskLog("Done");
    }
}