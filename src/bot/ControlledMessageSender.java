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
		this.timer = new Timer();
	}

	public void sendMessage(String message) {
		if(messageQueue.size() > MAX_QUEUE_SIZE)
			return;
		
		messageQueue.add(message);
		if (!task.isRunning()) {
			task = new ChatExecutor();
			task.activate();
		}
	}

	private class ChatExecutor extends TimerTask {

		private boolean isRunning = false;

		@Override
		public void run() {
			if (messageQueue.isEmpty()) {
				isRunning = false;
				cancel();
				return;
			}
			bot.sendInstantMessage(messageQueue.remove());
		}

		public boolean isRunning() {
			return isRunning;
		}
		
		public void activate() {
			isRunning = true;
			timer.schedule(task, 0, CHAT_COOLDOWN);
		}
	}
}
