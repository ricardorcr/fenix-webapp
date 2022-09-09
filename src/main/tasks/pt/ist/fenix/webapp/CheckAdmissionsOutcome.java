package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class CheckAdmissionsOutcome extends CustomTask {

    @Override
    public void runTask() throws Exception {



        final Application application = FenixFramework.getDomainObject("571415333968005");
        final boolean mandatoryActivitiesDone = Utils.allMandatoryActivitiesDone(application);
        taskLog("All done: %s%n", mandatoryActivitiesDone);

    }



}