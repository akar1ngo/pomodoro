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

public class TimerCommands {

  public static void register(App app, TimerService timerService) {
    registerSetTimer(app, timerService);
    registerStartTimer(app, timerService);
    registerStopTimer(app, timerService);
  }

  /**
   * Registers command to set timer duration within a channel.
   */
  private static void registerSetTimer(App app, TimerService timerService) {
    app.command("/set-timer", (req, ctx) -> {
      var channelId   = req.getPayload().getChannelId();
      var durationStr = req.getPayload().getText();

      int duration;
      try {
        duration = Integer.parseInt(durationStr);
        if (duration < 1 || duration > 60) {
          return ctx.ack("Provide a duration between 1 and 60 minutes.");
        }
      } catch (NumberFormatException e) {
        return ctx.ack("Provide a valid integral duration.");
      }

      timerService.setTimerDuration(channelId, duration);

      var resp = String.format("Timer set to %d minutes", duration);
      ctx.client().chatPostMessage(r -> r.channel(channelId).text(resp));

      return ctx.ack();
    });
  }

  /**
   * Registers command to start a timer within a channel.
   */
  private static void registerStartTimer(App app, TimerService timerService) {
    app.command("/start-timer", (req, ctx) -> {
      var channelId = req.getPayload().getChannelId();

      var tasks = timerService.getTasks(channelId);
      if (tasks == null || tasks.isEmpty()) {
        var resp = new StringBuilder()
          .append("No tasks submitted in this channel.")
          .append(System.lineSeparator())
          .append("Users can submit tasks using /task [description]")
          .toString();
        return ctx.ack(resp);
      }

      if (timerService.isTimerRunning(channelId)) {
        return ctx.ack("A task is currently running.");
      }

      // Notify users
      List<LayoutBlock> blocks = new ArrayList<>();
      var duration = timerService.getTimerDuration(channelId);
      blocks.add(section(s -> s.text(
        markdownText("*Pomodoro started for " + duration + " minutes*")))
      );
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
      ctx.client().chatPostMessage(r -> r.channel(channelId).blocks(blocks).text("Started"));

      // Schedule the timer
      timerService.startTimer(channelId, () -> {
        try {
          ctx
            .client()
            .chatPostMessage(r -> r.channel(channelId).text("*Time's up!*"));
        } catch (Exception e) {
          ctx.logger.error("Caught exception: %s", e.getMessage());
        }
      });

      return ctx.ack();
    });
  }

  /**
   * Registers command to stop a running timer within a channel.
   */
  private static void registerStopTimer(App app, TimerService timerService) {
    app.command("/stop-timer", (req, ctx) -> {
      var channelId = req.getPayload().getChannelId();
      var userId    = req.getPayload().getUserId();

      if (timerService.isTimerRunning(channelId)) {
        timerService.stopTimer(channelId);

        var resp = String.format("Timer stopped by <@%s>", userId);
        ctx.client().chatPostMessage(r -> r.channel(channelId).text(resp));

        return ctx.ack();
      }

      return ctx.ack("No timers are running");
    });
  }
}
