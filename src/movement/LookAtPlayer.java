package movement;

import java.util.TimerTask;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;

import bot.MinecraftBot;
import bot.Position;

public class LookAtPlayer extends Movement {

	public LookAtPlayer() {
		super(100);
	}

	@Override
	public TimerTask getTask(MinecraftBot bot) {
		return new TimerTask() {
			@Override
			public void run() {
				// get position of nearest player
				Position lookAt = bot.getPlayerTracker().getClosestPlayerLocation();
				if (lookAt == null)
					return;

				// calculate rotation
				double distance = bot.getPosition().distance(lookAt);
				double dx = lookAt.getX() - bot.getPosition().getX();
				double dy = lookAt.getY() - bot.getPosition().getY();
				double dz = lookAt.getZ() - bot.getPosition().getZ();

				float yaw = (float) (-Math.atan2(dx, dz) / Math.PI * 180);
				float pitch = (float) (-Math.asin(dy / distance) / Math.PI * 180);

				// rotate the bot
				if (!(Float.compare(bot.getPosition().getYaw(), yaw) == 0
						&& Float.compare(bot.getPosition().getPitch(), pitch) == 0)) {
					bot.getSession().send(new ServerboundMovePlayerRotPacket(true, yaw, pitch));
					bot.getPosition().rot(yaw, pitch);
				}

				// if player is looking back at the bot, start sneaking
				boolean pitchCheck = Math.abs(lookAt.getPitch() + pitch) < 6;
				boolean yawCheck = Math.abs(Math.abs(lookAt.getYaw() - yaw) - 180) < 4;
				if (pitchCheck && yawCheck) {
					bot.getMovementManager().startShiftCycle();
				}
			}
		};
	}
}
