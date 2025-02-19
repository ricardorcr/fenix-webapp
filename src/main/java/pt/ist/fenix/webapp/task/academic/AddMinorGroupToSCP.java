package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.service.MinorEnrolmentService;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class AddMinorGroupToSCP extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Application application = FenixFramework.getDomainObject("852890310678484");
        final CurriculumGroup curriculumGroup = MinorEnrolmentService.enrol(application);
    }
}
