package movement;

import bot.MinecraftBot;

public class MovementManager {
	
	private MinecraftBot bot;
	
	private ShiftCycle ss;
	private LookAtPlayer lap;
	private EyeContact ec;
	
	public MovementManager(MinecraftBot bot) {
		this.bot = bot;
		this.ss = new ShiftCycle();
		this.ec = new EyeContact();
		this.lap = new LookAtPlayer();
	}
	
	public void startShiftCycle() {
		if(ss.isRunning())
			return;
		
		ss.start();
		ss.getTimer().schedule(ss.getTask(bot), 0);
	}
	
	public void startLookAtPlayer() {
		if(lap.isRunning())
			return;
		
		lap.start();
		lap.getTimer().schedule(lap.getTask(bot), 0, lap.getCycleSpeed());
	}
	
	public void startEyeContact() {
		if(ec.isRunning())
			return;
		
		ec.start();
		ec.getTimer().schedule(ec.getTask(bot), 0, ec.getCycleSpeed());
	}
	
	public void stopAll() {
		ss.getTimer().cancel();
		lap.getTimer().cancel();
		ec.getTimer().cancel();
	}

}
