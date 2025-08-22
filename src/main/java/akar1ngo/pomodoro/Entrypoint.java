package akar1ngo.pomodoro;

import akar1ngo.pomodoro.commands.TaskCommands;
import akar1ngo.pomodoro.commands.TimerCommands;
import akar1ngo.pomodoro.services.TimerService;
import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageDeletedEvent;
import com.slack.api.model.event.MessageEvent;

public class Entrypoint {

  public static void main(String[] args) throws Exception {
    var app = new App();
    var timerService = new TimerService();

    // Register commands
    TaskCommands.register(app, timerService);
    TimerCommands.register(app, timerService);
    // Ack messages to remove useless warnings
    app.event(MessageEvent.class, (payload, ctx) -> ctx.ack());
    app.event(MessageChangedEvent.class, (payload, ctx) -> ctx.ack());
    app.event(MessageDeletedEvent.class, (payload, ctx) -> ctx.ack());

    var server = new SocketModeApp(app);
    server.start();
  }
}
