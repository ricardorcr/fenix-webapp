package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.DueDateAmountMap;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class CorrectDueDateImprovementEvents extends CustomTask {

    @Override
    public void runTask() throws Exception {

        Stream<String> idStream = Stream.of("571200585596929", "571200585596930", "571200585596931", "852675562307752", "852675562307753",
                "852675562307756", "852675562307757", "852675562307759", "852675562307761", "1134150539018311", "1134150539018312", "1134150539018313",
                "1134150539018314", "1134150539018316", "1415625515728925", "1415625515728926", "1697100492439671", "1697100492439672", "1697100492439673",
                "1697100492439674", "1978575469150209", "1978575469150210", "1978575469150213", "1978575469150214", "1978575469150215", "1978575469150216",
                "1978575469150219", "1978575469150220", "1978575469150221", "1978575469150222", "1978575469150225", "1978575469150226", "1978575469150227",
                "1978575469150229");

        idStream.map(s -> (Event) FenixFramework.getDomainObject(s)).forEach(e -> correct(e));
    }

    private void correct(final Event event) {
        if (!event.getRefundSet().isEmpty()) {
            taskLog("We have to do more stuff: %s%n", event.getExternalId());
        } else {
            taskLog("Going to change due map for event %s %s%n", event.getExternalId(), event.getDueDateAmountMap().toJson().toString());
            //martelar dueDateValueMap
            Method[] methodsDue = getAllDueMethodsInHierarchy(EnrolmentEvaluationEvent.class);
            Map<LocalDate, Money> dueMap = new HashMap<>();
            LocalDate dueDate = new LocalDate(2018, 9, 21); //data de fim do periodo de inscrição em melhoria
            dueMap.put(dueDate, new Money(10));
            DueDateAmountMap dueDateAmountMap = new DueDateAmountMap(dueMap);
            try {
                for(Method method : methodsDue){
                    if(method.getName().contains("Map")){
                        method.setAccessible(true);
                        method.invoke(event, dueDateAmountMap);
                    } else {
                        method.setAccessible(true);
                        method.invoke(event, dueDate);
                    }
                }
            } catch (IllegalAccessException e) {
                taskLog("Wrong 1");
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                taskLog("Wrong 2");
                e.printStackTrace();
            }
            taskLog("Evento %s corrigido para aluno %s%n", event.getExternalId(), event.getPerson().getUsername());
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
            if(method.getName().contains("setDue")) {
                allMethods.add(method);
            }
        }
        for (Method method : methods){
            if(method.getName().contains("setDue")) {
                allMethods.add(method);
            }
        }
        return allMethods.toArray(new Method[allMethods.size()]);
    }
}
