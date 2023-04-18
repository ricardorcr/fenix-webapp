package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituation;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DiplomaSupplementRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.RegistryDiplomaRequest;
import org.fenixedu.academic.dto.serviceRequests.AcademicServiceRequestBean;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RejectCancelAcademicRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        RegistryDiplomaRequest diplomaRequest = FenixFramework.getDomainObject("568112504113848");
        DiplomaSupplementRequest supplementRequest = FenixFramework.getDomainObject("568022309800534");

        final Person responsible = Person.findByUsername("ist23978");
        AcademicServiceRequestBean requestBean = new AcademicServiceRequestBean(responsible,"Criado por engano.");

        requestBean.setAcademicServiceRequestSituationType(AcademicServiceRequestSituationType.REJECTED);
        requestBean.setSituationDate(new YearMonthDay());

        Method method = null;
        try {
            method = AcademicServiceRequestSituation.class.getDeclaredMethod("create", new Class[]{AcademicServiceRequest.class, AcademicServiceRequestBean.class});
            method.setAccessible(true);
            method.invoke(null, new Object[]{diplomaRequest, requestBean});
            method.invoke(null, new Object[]{supplementRequest, requestBean});
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }

//        AcademicServiceRequestSituation.create(diplomaRequest, requestBean);
//        new AcademicServiceRequestSituation(diplomaRequest, requestBean)
    }
}
