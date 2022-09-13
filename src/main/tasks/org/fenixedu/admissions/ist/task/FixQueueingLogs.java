package org.fenixedu.admissions.ist.task;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.queueing.domain.AttendanceSlotAction;
import org.fenixedu.queueing.domain.QueueingLog;
import org.fenixedu.queueing.domain.QueueingSystem;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;

public class FixQueueingLogs extends CustomTask {

  @Override
  public void runTask() throws Exception {
    final DateTime start = new DateTime("2022-09-05");
    final DateTime end = new DateTime("2022-09-12");
    QueueingSystem.getInstance().getAttendanceQueueSet().stream().forEach(q -> {
      Set<QueueingLog> logs = q.getLogSet().stream().filter(s -> s.getWhen().isAfter(start) && s.getWhen().isBefore(end)).collect(Collectors.toSet());
      final Map<String, List<QueueingLog>> logsBySlotId = logs.stream().collect(Collectors.groupingBy(QueueingLog::getSlotId));
      logsBySlotId.values().forEach(logsForSlot -> {
        long numberOfTakes = logsForSlot.stream().filter(log -> log.getType() == AttendanceSlotAction.TAKE).count();
        long numberOfFUnTakes = logsForSlot.stream().filter(log -> log.getType() == AttendanceSlotAction.UN_TAKE)
            .count();
        if (numberOfTakes - numberOfFUnTakes > 1) {
          final List<QueueingLog> sortedTakes = logsForSlot.stream()
              .filter(l -> l.getType() == AttendanceSlotAction.TAKE).sorted((a, b) -> {
                if (a.getWhen().isAfter(b.getWhen())) {
                  return -1;
                } else if (b.getWhen().isAfter(a.getWhen())) {
                  return 1;
                } else {
                  return 0;
                }
              }).collect(Collectors.toList());
          System.out.println("Log " + sortedTakes.get(0).getExternalId());
          QueueingLog log = sortedTakes.get(0);
          log.setType(AttendanceSlotAction.FINISH);
        }
      });
    });
  }
}