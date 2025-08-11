package akar1ngo.pomodoro.models;

public class UserTask {
  private final String userId;
  private final String taskDescription;

  public UserTask(String userId, String taskDescription) {
    this.userId = userId;
    this.taskDescription = taskDescription;
  }

  public String getUserId() {
    return userId;
  }

  public String getTaskDescription() {
    return taskDescription;
  }
}
