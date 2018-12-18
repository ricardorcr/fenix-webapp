package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.DueDateAmountMap;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CorrectWrongAdvancementsInImprovementEvent extends CustomTask {

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

    @Override
    public void runTask() throws Exception {

        Map<String, String> map = new HashMap<>();
//        First batch
//        map.put("1978575469150257", "1127420325284594");
//        map.put("1415599745928231", "845945348581489");
//        map.put("1415625515729162", "1971845255421466");
//        map.put("1415625515729058", "1408895301991107");
//        map.put("1407400653364022", "1971845255421404");
//        map.put("1407400653363680", "1127420325285730");
//        map.put("1407400653363335", "845945348581426");
//        map.put("1407400653362742", "1127420325285791");
//        map.put("1407400653362093", "1971845255421375");
//        map.put("571200585597040", "1127420325286224");
//        map.put("1407400653360825", "1408895301991105");
//        map.put("1407400653360657", "1971845255421372");
//        map.put("1407400653360376", "1971845255421366");
//        map.put("1407400653359599", "845945348581484");

//        Second batch
//        map.put("1415625515729162", "1971845255421464");
//        map.put("1407400653364022", "1971845255421402");
//        map.put("1407400653363686", "845945348581421");
//        map.put("1407400653361084", "845945348581434");
//        map.put("1407400653361084", "845945348581432");

//        Third batch
        map.put("1407400653359737", "845945348581983");
        map.put("1407400653359800", "1690370278717199");
        map.put("1407400653361513", "1971845255421553");


        map.forEach((destinEventId, originPaymentId) -> {
            Event destinEvent = FenixFramework.getDomainObject(destinEventId);
            AccountingTransaction originPayment = FenixFramework.getDomainObject(originPaymentId);
            Event originEvent = originPayment.getEvent();

            //disconnect payments from destinEvent, keep the one that is connected to the refund
            List<AccountingTransaction> destinPayments = new ArrayList<>(destinEvent.getAccountingTransactions());
            destinEvent.getAccountingTransactionsSet().stream().forEach(ac -> ac.setEvent(null));
            //cancel the refund in originEvent
            if (originEvent.getRefundSet().size() != 1) {
//                taskLog("Why?? %s%n", originEvent.getExternalId());
                throw new Error("Event " + originEvent.getExternalId() + " has more than 1 or none refund");
            } else {
                taskLog("Origin event %s %s%n", originEvent.getExternalId(), originEvent.getDueDateAmountMap().toJson().toString());
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
                            method.invoke(originEvent, dueDateAmountMap);
                        } else {
                            method.setAccessible(true);
                            method.invoke(originEvent, dueDate);
                        }
                    }
                } catch (IllegalAccessException e) {
                    taskLog("Wrong 1");
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    taskLog("Wrong 2");
                    e.printStackTrace();
                }

                Refund refund = originEvent.getRefundSet().iterator().next();
                final AccountingTransaction paymentToDelete = refund.getAccountingTransaction();
                //we need to connect it before deleting it
                paymentToDelete.setEvent(destinEvent);
                refund.delete();

                //reconnect the payments except the one that was refered in the refund
                destinPayments.stream().filter(ac -> ac != paymentToDelete).forEach(ac -> ac.setEvent(destinEvent));
                taskLog("Origin event AFTER %s %s%n", originEvent.getExternalId(), originEvent.getDueDateAmountMap().toJson().toString());
                taskLog("All is ok: %s %s%n", destinEvent.getPerson().getUsername(), destinEvent.getExternalId());
            }
        });
    }
}
