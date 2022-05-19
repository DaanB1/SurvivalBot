package bot;

import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

public class AutoReconnect {

	private MinecraftBot bot;
	private Instant latestAttempt;
	private Timer timer;

	private boolean waiting = false;
	private boolean onCooldown = false;
	private int attempts = 0;

	private static final int SMALL_COOLDOWN = 30;
	private static final int LARGE_COOLDOWN = 1800; // 30 minutes

	public AutoReconnect(MinecraftBot bot) {
		this.bot = bot;
		this.latestAttempt = Instant.now();
		this.timer = new Timer();
	}

	public void reconnect() {
		System.out.println("Attempting to reconnect..");
		if (attempts > 4) {
			triggerCooldown();
			return;
		}

		if (Duration.between(latestAttempt, Instant.now()).getSeconds() < SMALL_COOLDOWN) {
			scheduleReconnect();
			return;
		}

		attempts++;
		latestAttempt = Instant.now();
		try {
			bot.connect(bot.getHost());
		} catch (Exception e) {
			// TODO: Catch authentication error and re-authenticate
			reconnect();
		}
	}

	public void clearAttempts() {
		attempts = 0;
	}

	private void scheduleReconnect() {
		if (waiting)
			return;

		System.out.println("Scheduling reconnect...");
		waiting = true;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				waiting = false;
				reconnect();
			}
		};
		int duration = SMALL_COOLDOWN - (int) Duration.between(latestAttempt, Instant.now()).getSeconds() + 1;
		if (duration < 0)
			duration = 0;

		timer.schedule(task, duration * 1000);
	}

	private void triggerCooldown() {
		if (onCooldown)
			return;

		System.out.println("Reconnect timeout");
		onCooldown = true;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				onCooldown = false;
				attempts = 0;
				reconnect();
			}
		};
		timer.schedule(task, LARGE_COOLDOWN * 1000);
	}

}
