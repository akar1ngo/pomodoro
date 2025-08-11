package akar1ngo.pomodoro.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.slack.api.bolt.App;
import com.slack.api.model.block.LayoutBlock;
import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;

import akar1ngo.pomodoro.models.UserTask;
import akar1ngo.pomodoro.services.TimerService; 

public class TaskCommands {

  public static void register(App app, TimerService timerService) {
    registerTask(app, timerService);
    registerListTasks(app, timerService);
    registerClearTasks(app, timerService);
  }

  /**
   * Registers command to create new tasks.
   */
  private static void registerTask(App app, TimerService timerService) {
    app.command("/task", (req, ctx) -> {
      var channelId       = req.getPayload().getChannelId();
      var userId          = req.getPayload().getUserId();
      var taskDescription = req.getPayload().getText();

      timerService.submitTask(channelId, userId, taskDescription);

      var resp = String.format("Registered task: %s", taskDescription);

      return ctx.ack(resp);
    });
  }

  /**
   * Registers command to list all tasks in a rich text format.
   */
  private static void registerListTasks(App app, TimerService timerService) {
    app.command("/list-tasks", (req, ctx) -> {
      var channelId = req.getPayload().getChannelId();

      var tasks = timerService.getTasks(channelId);
      if (tasks == null || tasks.isEmpty()) {
        return ctx.ack("No tasks have been submitted.");
      }

      List<LayoutBlock> blocks = new ArrayList<>();
      blocks.add(section(s -> s.text(markdownText("*All tasks:*"))));
      tasks.stream()
        .collect(Collectors.groupingBy(UserTask::getUserId))
        .forEach((user, userTasks) -> {
          blocks.add(divider());
          blocks.add(section(s -> s.text(markdownText("*<@" + user + ">*"))));
          var taskList = userTasks
            .stream()
            .map(task -> "• " + task.getTaskDescription())
            .collect(Collectors.joining(System.lineSeparator()));
          blocks.add(section(s -> s.text(markdownText(taskList))));
      });

      return ctx.ack(r -> r.blocks(blocks));
    });
  }

  /**
   * Registers command to clear tasks submitted by the user. This does NOT clear
   * all tasks submitted within a channel.
   */
  private static void registerClearTasks(App app, TimerService timerService) {
    app.command("/clear-tasks", (req, ctx) -> {
      var channelId = req.getPayload().getChannelId();
      var userId    = req.getPayload().getUserId();

      timerService.clearTasks(channelId, userId);

      return ctx.ack("Tasks cleared");
    });
  }
}
