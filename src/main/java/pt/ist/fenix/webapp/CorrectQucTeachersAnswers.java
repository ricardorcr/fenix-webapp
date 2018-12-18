package pt.ist.fenix.webapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.quc.domain.InquiryQuestion;
import pt.ist.fenixedu.quc.domain.QuestionAnswer;

public class CorrectQucTeachersAnswers extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionSemester
                .readActualExecutionSemester()
                .getPreviousExecutionPeriod()
                .getAssociatedExecutionCoursesSet()
                .stream()
                .flatMap(ec -> ec.getProfessorshipsSet().stream())
                .map(p -> p.getInquiryTeacherAnswer())
                .filter(Objects::nonNull)
                .filter(i -> i.getNumberOfAnsweredQuestions() > 23)
                .forEach(
                        i -> {
                            Map<InquiryQuestion, List<QuestionAnswer>> duplicateAnswers =
                                    new HashMap<InquiryQuestion, List<QuestionAnswer>>();
                            i.getQuestionAnswersSet()
                                    .stream()
                                    .forEach(
                                            qa -> {
                                                List<QuestionAnswer> answersList =
                                                        duplicateAnswers.getOrDefault(qa.getInquiryQuestion(),
                                                                new ArrayList<QuestionAnswer>());
                                                answersList.add(qa);
                                                duplicateAnswers.put(qa.getInquiryQuestion(), answersList);
                                            });
                            duplicateAnswers.forEach((k, v) -> {
                                int repeatedAnswers = v.size();
                                taskLog("Respostas repetidas: %s\n", repeatedAnswers);
                                long differentAnswers = v.stream().map(QuestionAnswer::getAnswer).distinct().count();
                                taskLog("Existem %s valores diferentes de resposta\n", differentAnswers);

                                if (differentAnswers == 1) {
                                    for (int iter = 0; iter < repeatedAnswers - 1; iter++) {
                                        taskLog("Going to delete answer %s from inquiryAnswer %s\n", v.get(iter).getExternalId(),
                                                v.get(iter).getInquiryAnswer());
                                        v.get(iter).delete();
                                    }
                                }
                            });
                        });
    }
}
