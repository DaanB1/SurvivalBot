package movement;

import java.util.TimerTask;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import bot.MinecraftBot;

public class ShiftCycle extends Movement {

	@Override
	public TimerTask getTask(MinecraftBot bot) {
		return new TimerTask() {
			@Override
			public void run() {
				try {
					bot.getSession()
							.send(new ServerboundPlayerCommandPacket(bot.getEntityId(), PlayerState.START_SNEAKING));
					Thread.sleep(200);
					bot.getSession()
							.send(new ServerboundPlayerCommandPacket(bot.getEntityId(), PlayerState.STOP_SNEAKING));
					Thread.sleep(300);
					stop();
				} catch (InterruptedException e) {

				}
			}
		};
	}
}
