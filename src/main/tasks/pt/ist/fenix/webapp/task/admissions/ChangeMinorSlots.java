package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.ist.wizard.RemoteReader;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ChangeMinorSlots extends CustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("852907490541640");
        process.setFormData(string("test/minorHack.json"));
    }

}