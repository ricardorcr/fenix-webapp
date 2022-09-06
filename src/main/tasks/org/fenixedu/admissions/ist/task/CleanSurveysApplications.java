package org.fenixedu.admissions.ist.task;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class CleanSurveysApplications extends CustomTask {

    @Override
    public void runTask() throws Exception {

        for(AdmissionProcess admissionProcess: AdmissionsSystem.getInstance().getAdmissionProcessSet()){
            for(AdmissionProcessTarget admissionProcessTarget: admissionProcess.getAdmissionProcessTargetSet()){
                for(Application application: admissionProcessTarget.getApplicationSet()){
                    JsonObject data = application.getDataObject();
                    data.remove("surveys");
                    application.setData(data.toString());
                }
            }
        }
    }

}