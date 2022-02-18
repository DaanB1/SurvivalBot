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
	private ChatExecutor task;
	private Timer timer;
	private Queue<String> messageQueue;
	
	private static final int CHAT_COOLDOWN = 3000;
	private static final int MAX_QUEUE_SIZE = 5;

	public ControlledMessageSender(MinecraftBot bot) {
		this.bot = bot;
		this.messageQueue = new LinkedList<>();
		this.task = new ChatExecutor();
	}

	public void sendMessage(String message) {
		if(messageQueue.size() > MAX_QUEUE_SIZE)
			return;
		
		messageQueue.add(message);
		if (!task.isRunning()) {
			task = new ChatExecutor();
			timer = new Timer();
			timer.schedule(task, 0, CHAT_COOLDOWN);
		}
	}

	private class ChatExecutor extends TimerTask {

		private boolean isRunning = false;

		@Override
		public void run() {
			isRunning = true;
			if (messageQueue.isEmpty()) {
				timer.cancel();
				this.cancel();
				isRunning = false;
				return;
			}
			String message = messageQueue.remove();
			bot.sendMessage(message);
		}

		public boolean isRunning() {
			return isRunning;
		}
	}
}
