package bot;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Sends out messages at a controlled pace to prevent spam
 */
public class ControlledMessageSender {

	private MinecraftBot bot;
	private Queue<String> messageQueue;
	private TimerTask task;

	public ControlledMessageSender(MinecraftBot bot) {
		this.bot = bot;
		this.messageQueue = new LinkedList<>();
	}

	public void sendMessage(String message) {
		if (messageQueue.isEmpty()) {
			bot.sendMessage(message);
			return;
		}
		messageQueue.add(message);
		if (task == null) {
			task = new TimerTask() {
				@Override
				public void run() {
					if (messageQueue.isEmpty()) {
						this.cancel();
						return;
					}
					String message = messageQueue.remove();
					bot.sendMessage(message);
				}
			};
			Timer timer = new Timer();
			timer.schedule(task, 0, 3000);
		}
	}

}
