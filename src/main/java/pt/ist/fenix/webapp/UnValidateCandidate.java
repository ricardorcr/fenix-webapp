package pt.ist.fenix.webapp;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class UnValidateCandidate extends CustomTask {

    @Override
    public void runTask() throws Exception {            
//        Identity identity = FenixFramework.getDomainObject("1697345305577744");
//        identity.getCandidateSet().forEach(c -> {
//            if (c instanceof ExternalCandidate) {
//                ExternalCandidate candidate = (ExternalCandidate) c;
//                candidate.setIsIdentityValidated(false);
//                candidate.setIdentity(null);                
//            } else {
//                InternalCandidate internalCandidate = (InternalCandidate)c;
//                internalCandidate.setIdentity(null);
//                internalCandidate.setAdmissionsSystem(null);
//                internalCandidate.setUser(null);
//                Method[] methods = getAllMethodsInHierarchy(InternalCandidate.class);
//                Optional<Method> findAny = Arrays.asList(methods).stream()                        
//                        .filter(m -> m.getName().equals("deleteDomainObject"))
//                        .findAny();
//                
//                try {
//                    Method method = findAny.get();
//                    method.setAccessible(true);
//                    method.invoke(internalCandidate, null);
//                } catch (IllegalAccessException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (IllegalArgumentException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        });
//        identity.getPersonSet().clear();
//        identity.delete();
    }   
    
    public static Method[] getAllMethodsInHierarchy(Class<?> objectClass) {
        Set<Method> allMethods = new HashSet<Method>();
        Method[] declaredMethods = objectClass.getDeclaredMethods();
        Method[] methods = objectClass.getMethods();
        if (objectClass.getSuperclass() != null) {
            Class<?> superClass = objectClass.getSuperclass();
            Method[] superClassMethods = getAllMethodsInHierarchy(superClass);
            allMethods.addAll(Arrays.asList(superClassMethods));
        }
        allMethods.addAll(Arrays.asList(declaredMethods));
        allMethods.addAll(Arrays.asList(methods));
        return allMethods.toArray(new Method[allMethods.size()]);
    }
}