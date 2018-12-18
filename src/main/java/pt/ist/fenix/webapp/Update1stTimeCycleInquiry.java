package pt.ist.fenix.webapp;

import com.google.common.primitives.Ints;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryGroupQuestion;
import pt.ist.fenixedu.quc.domain.InquiryQuestion;
import pt.ist.fenixedu.quc.domain.InquiryQuestionHeader;
import pt.ist.fenixedu.quc.domain.InquiryTemplate;
import pt.ist.fenixedu.quc.domain.Student1rstCycleInquiryTemplate;
import pt.ist.fenixedu.quc.domain.Student2ndCycleInquiryTemplate;
import pt.ist.fenixedu.quc.domain.StudentOtherCycleInquiryTemplate;
import pt.ist.fenixframework.Atomic;

import java.util.Collection;

public class Update1stTimeCycleInquiry extends CustomTask {

    @Override
    public void runTask() throws Exception {

        taskLog("2nd Cycle Inquiry");
//        updateInquiry(Student1rstCycleInquiryTemplate.getCurrentTemplate());
        updateInquiry(Student2ndCycleInquiryTemplate.getCurrentTemplate());
//        updateInquiry(StudentOtherCycleInquiryTemplate.getCurrentTemplate());

//        throw new RuntimeException("Dry run");
    }

    private void updateInquiry(InquiryTemplate inquiryTemplate) {
        for (InquiryBlock inquiryBlock : inquiryTemplate.getInquiryBlocksSet()) {
            for (InquiryGroupQuestion inquiryGroupQuestion : inquiryBlock.getInquiryGroupsQuestionsSet()) {
                if (inquiryGroupQuestion.getInquiryQuestionHeader() != null) {
                    LocalizedString groupTitle = inquiryGroupQuestion.getInquiryQuestionHeader().getTitle();
                    taskLog("Grupo %s%n", groupTitle.getContent());
                    LocalizedString updatedQuestion = getUpdatedQuestion(groupTitle);
                    if (updatedQuestion != null) {
                        inquiryGroupQuestion.getInquiryQuestionHeader().setTitle(updatedQuestion);
//                        if (updatedQuestion.getContent().startsWith("6.")) {
//                            inquiryGroupQuestion.setRequired(false);
//                        }
                    }
                }
                for (InquiryQuestion inquiryQuestion : inquiryGroupQuestion.getInquiryQuestionsSet()) {
                    if (inquiryQuestion.getLabel().getContent().startsWith("5.")) {
                        taskLog(inquiryQuestion.getLabel().getContent());
                        inquiryGroupQuestion.setInquiryBlock(null);
                    } else {
                        taskLog(inquiryQuestion.getLabel().getContent());
                        LocalizedString updatedQuestion = getUpdatedQuestion(inquiryQuestion.getLabel());
                        if (updatedQuestion != null) {
                            inquiryQuestion.setLabel(updatedQuestion);
                        }
                    }
                }
            }
        }
    }

    private LocalizedString getUpdatedQuestion(final LocalizedString question) {
        String[] strings = question.getContent().split(" ");
        String questionNumber = null;
        if (strings[0].endsWith(".")) {
            questionNumber = strings[0].replace(".", "");
        } else if (strings[0].contains(".")) {
            questionNumber = strings[0].substring(0, strings[0].indexOf("."));
        } else {
            return null;
        }
        Integer number = Ints.tryParse(questionNumber);
        if (number != null && number > 4) {
            Integer newNumber = number - 1;
            taskLog("changing the number for: %s - %s%n", newNumber, question.getContent());
            LocalizedString newLocalizedString =
                    new LocalizedString(LocaleUtils.PT, question.getContent(LocaleUtils.PT).replace(number.toString(), newNumber.toString()));
            newLocalizedString.with(LocaleUtils.EN, question.getContent(LocaleUtils.EN).replace(number.toString(), newNumber.toString()));
            taskLog("NEW %s%n", newLocalizedString.getContent());
            return newLocalizedString;
        } else {
            taskLog("Not changing for: %s%n", question.getContent());
            return null;
        }
    }
}