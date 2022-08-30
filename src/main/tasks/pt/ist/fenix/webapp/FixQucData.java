package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.quc.domain.StudentInquiryRegistry;
import pt.ist.fenixframework.FenixFramework;

public class FixQucData extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final StudentInquiryRegistry inquiryRegistry = FenixFramework.getDomainObject("568868418504737");
        inquiryRegistry.setRegistration(FenixFramework.getDomainObject("283734129592468"));
    }


}