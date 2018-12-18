package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.postingRules.AdministrativeOfficeFeePR;
import org.fenixedu.academic.domain.accounting.postingRules.AdministrativeOfficeFeePR_Base;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

//TODO DOES NOT WORK!!!!!!!!!!!!
public class CorrectAdminFeePR extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdministrativeOfficeFeePR adminPRtoDelete = FenixFramework.getDomainObject("1126393828081665");
        adminPRtoDelete.delete();

        AdministrativeOfficeFeePR adminPRtoEdit = FenixFramework.getDomainObject("281968897949699");
        //final Method method = AdministrativeOfficeFeePR.class.getMethod("setEndDate", null);

        DateTime endDate = null;
        Method[] methodsDue = getAllDueMethodsInHierarchy(AdministrativeOfficeFeePR_Base.class);
        for(Method method : methodsDue){
            if (method.getDeclaringClass().getCanonicalName().contains("_Base")) {
                method.setAccessible(true);
                method.invoke(adminPRtoEdit, endDate);
            }
        }
    }

    public static Method[] getAllDueMethodsInHierarchy(Class<?> objectClass) {
        Set<Method> allMethods = new HashSet<Method>();
        Method[] declaredMethods = objectClass.getDeclaredMethods();
        Method[] methods = objectClass.getMethods();
        if (objectClass.getSuperclass() != null) {
            Class<?> superClass = objectClass.getSuperclass();
            Method[] superClassMethods = getAllDueMethodsInHierarchy(superClass);
            allMethods.addAll(Arrays.asList(superClassMethods));
        }
        for (Method method : declaredMethods){
            if(method.getName().contains("setEndDate")) {
                allMethods.add(method);
            }
        }
        for (Method method : methods){
            if(method.getName().contains("setEndDate")) {
                allMethods.add(method);
            }
        }
        return allMethods.toArray(new Method[allMethods.size()]);
    }
}
