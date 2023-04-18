package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.ist.wizard.RemoteReader;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class UpdateForm extends WriteCustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("1978807397384213");
        admissionProcess.setFormData(string("directIngressionFormData.json"));
    }
}