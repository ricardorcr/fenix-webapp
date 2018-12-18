package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.quc.domain.InquiryGroupQuestion;
import pt.ist.fenixedu.quc.domain.InquiryQuestion;
import pt.ist.fenixframework.FenixFramework;

public class Fix1stTime1stCycleInquiry extends CustomTask {

    @Override
    public void runTask() throws Exception {
        InquiryGroupQuestion inquiryGroupQuestion = FenixFramework.getDomainObject("5939939773573");//group 5 to remove
        InquiryQuestion inquiryQuestion = FenixFramework.getDomainObject("5914169969607");//question of group 5 to remove

        inquiryQuestion.setInquiryGroupQuestion(inquiryGroupQuestion);
        inquiryGroupQuestion.setInquiryBlock(null);
    }
}
