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

public class HackDueDateAmount extends CustomTask {

    @Override
    public void runTask() throws Exception {

        final Event event = FenixFramework.getDomainObject("569199130838804");

        final DueDateAmountMap dueDateAmountMap = event.getDueDateAmountMap();
        taskLog("Going to change due map for event %s %s%n", event.getExternalId(), dueDateAmountMap.toJson().toString());
        //martelar dueDateValueMap
        Method[] methodsDue = getAllDueMethodsInHierarchy(EnrolmentEvaluationEvent.class);
        Map<LocalDate, Money> dueMap = new HashMap<>();
        LocalDate dueDate = new LocalDate(2022, 12, 16);
        dueMap.put(dueDate, new Money(1375));
        dueDateAmountMap.entrySet().stream()
//                .filter(e -> !(e.getKey().getYear() == 2022 && e.getKey().getDayOfMonth() == 31))
                .forEach(e -> dueMap.put(e.getKey(), e.getValue()));
        DueDateAmountMap newDueDateMap = new DueDateAmountMap(dueMap);
        taskLog("New Due Date Map: %s%n", newDueDateMap.toJson().toString());
        try {
            for(Method method : methodsDue){
                if (method.getName().contains("Map")){
                    method.setAccessible(true);
                    method.invoke(event, newDueDateMap);
                } else {
//                    method.setAccessible(true);
//                    method.invoke(event, dueDate);
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
            if (method.getName().contains("setDue")) {
                allMethods.add(method);
            }
        }
        for (Method method : methods){
            if (method.getName().contains("setDue")) {
                allMethods.add(method);
            }
        }
        return allMethods.toArray(new Method[allMethods.size()]);
    }
}
