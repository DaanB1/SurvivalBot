package movement;

import java.util.Timer;
import java.util.TimerTask;

import bot.MinecraftBot;

public abstract class Movement {
	
	private Timer timer;
	private boolean isRunning;
	private int cycleSpeed;
	
	public Movement() {
		this.timer = new Timer();
		this.cycleSpeed = 0;
	}
	
	public Movement(int cycleSpeed) {
		this.timer = new Timer();
		this.cycleSpeed = cycleSpeed;
	}
	
	public abstract TimerTask getTask(MinecraftBot bot);
	
	public Timer getTimer() {
		return timer;
	}

	public long getCycleSpeed() {
		return cycleSpeed;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	
	public void start() {
		isRunning = true;
	}
	
	public void stop() {
		isRunning = false;
	}

	
}
