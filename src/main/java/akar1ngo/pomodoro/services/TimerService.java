package akar1ngo.pomodoro.services;

import akar1ngo.pomodoro.models.UserTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerService {

  private Map<String, List<UserTask>> tasks = new ConcurrentHashMap<>();
  private Map<String, Integer> durations = new ConcurrentHashMap<>();
  private Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
  private ScheduledExecutorService scheduler;

  private static final List<UserTask> EMPTY_USER_TASKS = new ArrayList<>();

  public TimerService() {
    this(Executors.newScheduledThreadPool(1));
  }

  public TimerService(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Submits a user task to the specified channel.
   */
  public void submitTask(
    String channelId,
    String userId,
    String taskDescription
  ) {
    tasks
      .computeIfAbsent(channelId, key -> new CopyOnWriteArrayList<>())
      .add(new UserTask(userId, taskDescription));
  }

  /**
   * Retrieves the list of tasks for the specified channel.
   */
  public List<UserTask> getTasks(String channelId) {
    return tasks.getOrDefault(channelId, EMPTY_USER_TASKS);
  }

  /**
   * Clears tasks for a specific user within a channel.
   */
  public void clearTasks(String channelId, String userId) {
    tasks
      .getOrDefault(channelId, EMPTY_USER_TASKS)
      .removeIf(task -> task.getUserId().equals(userId));
  }

  /**
   * Sets the duration for the timer of a specific channel.
   */
  public void setTimerDuration(String channelId, int duration) {
    durations.put(channelId, duration);
  }

  /**
   * Retrieves the timer duration for the specified channel. Defaults to 30.
   */
  public int getTimerDuration(String channelId) {
    return durations.getOrDefault(channelId, 30);
  }

  /**
   * Checks if the timer is currently running for the specified channel.
   */
  public boolean isTimerRunning(String channelId) {
    var timer = timers.get(channelId);
    return timer != null && !timer.isDone();
  }

  /**
   * Starts the timer for a channel with an optional completion callback.
   */
  public void startTimer(String channelId, @Nullable Runnable onTimerComplete) {
    var duration = getTimerDuration(channelId);
    var timer = scheduler.schedule(
      () -> {
        tasks.remove(channelId);
        if (onTimerComplete != null) {
          onTimerComplete.run();
        }
      },
      duration,
      TimeUnit.MINUTES
    );
    timers.put(channelId, timer);
  }

  /**
   * Stops the timer for the specified channel and clears its tasks.
   */
  public void stopTimer(@Nonnull String channelId) {
    var timer = timers.get(channelId);
    if (timer != null && !timer.isDone()) {
      timer.cancel(true);
      timers.remove(channelId);
      tasks.remove(channelId);
    }
  }
}
