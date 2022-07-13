package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ChangeAdmissionsProcessURL extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("571432513831066");
        process.setInformationURL("https://tecnico.ulisboa.pt/pt/ensino/cursos/humanidades-artes-e-ciencias-sociais/");
    }

}