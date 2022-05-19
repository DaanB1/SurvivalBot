package movement;

import java.util.TimerTask;

import bot.MinecraftBot;
import bot.Position;

public class EyeContact extends Movement {
	
	public EyeContact() {
		super(100);
	}

	@Override
	public TimerTask getTask(MinecraftBot bot) {
		return new TimerTask() {

			@Override
			public void run() {
				Position myPos = bot.getPosition();
				for(Position p : bot.getPlayerTracker().getAllPositions()) {
					if(Math.abs(Math.abs(myPos.getYaw() - p.getYaw()) - 180) > 4)
						continue;
					
					double dx = myPos.getX() - p.getX();
					double dz = myPos.getZ() - p.getZ();		
					double yaw = -Math.atan2(dx, dz) / Math.PI * 180;
					if(Math.abs(yaw - p.getYaw()) < 5); {
						bot.getMovementManager().startShiftCycle();
						return;
					}
				}
			}
			
		};
	}
	
	

}
