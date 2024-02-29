package pt.ist.fenix.webapp.task.quc;

import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.quc.domain.QuestionAnswer;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.backend.jvstmojb.pstm.AbstractDomainObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DeleteProfessorshipAnswers extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Professorship professorship = FenixFramework.getDomainObject("1409569611844916");
        professorship.getInquiryStudentTeacherAnswersSet()
                .forEach(answer -> {
                    answer.setProfessorship(null);
                    answer.setRootDomainObject(null);
                    answer.setInquiryCourseAnswer(null);
                    answer.getQuestionAnswersSet()
                            .forEach(QuestionAnswer::delete);
                    try {
                        final Method method = AbstractDomainObject.class.getDeclaredMethod("deleteDomainObject");
                        method.setAccessible(true);
                        method.invoke(answer);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
