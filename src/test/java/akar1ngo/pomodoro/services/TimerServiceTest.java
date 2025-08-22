package akar1ngo.pomodoro.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimerServiceTest {

  private TimerService timerService;
  private ScheduledExecutorService testScheduler;

  @BeforeEach
  public void setup() {
    testScheduler = Executors.newSingleThreadScheduledExecutor();
    timerService = new TimerService(testScheduler);
  }

  @Test
  public void testSubmitAndGetTasks() {
    var channelId = "channel1";
    var userId = "user1";
    var taskDescription = "Test Task";

    timerService.submitTask(channelId, userId, taskDescription);

    var tasks = timerService.getTasks(channelId);
    assertEquals(1, tasks.size());

    var task = tasks.get(0);
    assertEquals(userId, task.getUserId());
    assertEquals(taskDescription, task.getTaskDescription());
  }

  @Test
  public void testTasksEmpty() {
    var channelId = "channel1";
    var tasks = timerService.getTasks(channelId);
    assertTrue(tasks.isEmpty());
  }

  @Test
  public void testClearTasks() {
    var channelId = "channel1";
    var userId1 = "user1";
    var userId2 = "user2";
    var taskDescription1 = "Task 1";
    var taskDescription2 = "Task 2";

    // Two users submit tasks
    timerService.submitTask(channelId, userId1, taskDescription1);
    timerService.submitTask(channelId, userId2, taskDescription2);
    // One user removes their tasks
    timerService.clearTasks(channelId, userId1);

    var tasks = timerService.getTasks(channelId);
    assertEquals(1, tasks.size());

    var task = tasks.get(0);
    assertEquals(userId2, task.getUserId());
    assertEquals(taskDescription2, task.getTaskDescription());
  }

  @Test
  public void testTimerDuration() {
    var channelId = "channel1";
    var duration = 45;

    timerService.setTimerDuration(channelId, duration);
    var returnedDuration = timerService.getTimerDuration(channelId);

    assertEquals(duration, returnedDuration);
  }

  @Test
  public void testDefaultDuration() {
    var channelId = "channel1";
    var defaultDuration = timerService.getTimerDuration(channelId);

    assertEquals(30, defaultDuration);
  }

  @Test
  public void testIsTimerRunning() throws InterruptedException {
    var channelId = "channel1";
    var duration = 1;
    timerService.setTimerDuration(channelId, duration);

    // Initially, no timer is running
    assertFalse(timerService.isTimerRunning(channelId));

    // Start the timer
    timerService.startTimer(channelId, null);
    assertTrue(timerService.isTimerRunning(channelId));

    // Stop the timer
    timerService.stopTimer(channelId);
    assertFalse(timerService.isTimerRunning(channelId));
  }

  @Test
  public void testStartTimer() throws InterruptedException {
    var channelId = "channel1";
    var duration = 0;

    timerService.setTimerDuration(channelId, duration);

    // Wait for timer to complete
    var latch = new CountDownLatch(1);
    timerService.startTimer(channelId, latch::countDown);

    // Since duration is zero, the timer should complete immediately
    var completed = latch.await(1, TimeUnit.SECONDS);
    assertTrue(completed);

    // Ensure the timer is no longer running
    assertFalse(timerService.isTimerRunning(channelId));
  }

  @Test
  public void testStopTimer() throws InterruptedException {
    var channelId = "channel1";
    var duration = 1;

    timerService.setTimerDuration(channelId, duration);

    var latch = new CountDownLatch(1);
    timerService.startTimer(channelId, latch::countDown);

    // Timer should be running
    assertTrue(timerService.isTimerRunning(channelId));

    // Stop the timer
    timerService.stopTimer(channelId);

    // Timer should not be running
    assertFalse(timerService.isTimerRunning(channelId));

    // Check if onTimerComplete is called
    var completed = latch.await(2, TimeUnit.SECONDS);
    assertFalse(completed, "Timer was stopped, but onTimerComplete was called");
  }

  @Test
  public void testTaskClearedAfterCompletion() throws InterruptedException {
    var channelId = "channel1";
    var userId = "user1";
    var taskDescription = "Test Task";

    timerService.submitTask(channelId, userId, taskDescription);
    timerService.setTimerDuration(channelId, 0);

    var latch = new CountDownLatch(1);
    timerService.startTimer(channelId, latch::countDown);

    // Wait for timer to complete
    var completed = latch.await(1, TimeUnit.SECONDS);
    assertTrue(completed);

    // Tasks should be cleared
    var tasks = timerService.getTasks(channelId);
    assertTrue(
      tasks.isEmpty(),
      "Tasks were not cleared after timer completion"
    );
  }

  @Test
  public void testMultipleChannels() {
    var channelId1 = "channel1";
    var channelId2 = "channel2";
    var userId = "user1";
    var taskDescription1 = "Task 1";
    var taskDescription2 = "Task 2";

    timerService.submitTask(channelId1, userId, taskDescription1);
    timerService.submitTask(channelId2, userId, taskDescription2);

    var tasks1 = timerService.getTasks(channelId1);
    var tasks2 = timerService.getTasks(channelId2);

    assertEquals(1, tasks1.size());
    assertEquals(taskDescription1, tasks1.get(0).getTaskDescription());

    assertEquals(1, tasks2.size());
    assertEquals(taskDescription2, tasks2.get(0).getTaskDescription());
  }

  @Test
  public void testTimerIsolation() throws InterruptedException {
    var channelId1 = "channel1";
    var channelId2 = "channel2";
    var duration = 1;

    timerService.setTimerDuration(channelId1, duration);
    timerService.setTimerDuration(channelId2, duration);

    var latch1 = new CountDownLatch(1);
    var latch2 = new CountDownLatch(1);

    timerService.startTimer(channelId1, latch1::countDown);
    timerService.startTimer(channelId2, latch2::countDown);

    timerService.stopTimer(channelId1);
    assertFalse(timerService.isTimerRunning(channelId1));
    assertTrue(timerService.isTimerRunning(channelId2));

    var completed1 = latch1.await(100, TimeUnit.MILLISECONDS);
    assertFalse(
      completed1,
      "Timer for channelId1 stopped, yet onTimerComplete ran"
    );

    // Simulate timer completion for channelId2
    timerService.stopTimer(channelId2);
    latch2.countDown(); // Manually count down for the test
    var completed2 = latch2.await(1, TimeUnit.SECONDS);
    assertTrue(completed2, "Timer for channelId2 did not complete as expected");
  }
}
