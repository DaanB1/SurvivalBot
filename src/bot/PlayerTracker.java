package bot;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;

//Makes the bot look at the closest player
public class PlayerTracker {

	private MinecraftBot bot;
	private HashMap<Integer, Position> nearbyPlayers;
	private Timer timer;
	private TimerTask task;

	private static final int MAX_DISTANCE = 15;

	public PlayerTracker(MinecraftBot bot) {
		this.bot = bot;
		this.nearbyPlayers = new HashMap<>();
		this.timer = new Timer();
	}

	public void start() {
		task = new TimerTask() {
			@Override
			public void run() {
				Position lookAt = getClosestPlayerLocation();
				if (lookAt == null)
					return;

				Position myLoc = bot.getPosition();
				double distance = myLoc.distance(lookAt);
				double dx = myLoc.getX() - lookAt.getX();
				double dy = myLoc.getY() - lookAt.getY();
				double dz = myLoc.getZ() - lookAt.getZ();

				try {
					// credits to stackoverflow for these formulas
					float pitch = (float) Math.asin(dy / distance);
					float yaw = (float) (Math.asin(dx / (Math.cos(pitch) * distance)) * (180 / Math.PI));
					pitch = (float) (pitch * (180 / Math.PI));

					// hot fix
					if (dz > 0 && yaw > 0)
						yaw = 180 - yaw;
					if (dz > 0 && yaw < 0)
						yaw = -180 - yaw;
					if (Float.isNaN(yaw) || Float.isNaN(pitch))
						return;

					bot.rotate(yaw, pitch);
					
					//if player is looking back at the bot, start sneaking
					boolean pitchCheck = Math.abs(lookAt.getPitch() + pitch) < 5;
					boolean yawCheck = Math.abs(lookAt.getYaw() - yaw - 180) % 360 < 2 || Math.abs(lookAt.getYaw() - yaw - 180) % 360 > 358;
					if (pitchCheck && yawCheck) {
						bot.startSneaking();
					} else {
						bot.stopSneaking();
					}
				} catch (Exception e) {
					// potential divide by 0 exception if Math.cos(pitch) == 0
				}
			}
		};
		timer.schedule(task, 100, 100);
	}

	public void addPlayer(ClientboundAddPlayerPacket packet) {
		if (packet.getEntityId() == bot.getEntityId())
			return;

		Position loc = new Position(packet.getX(), packet.getY(), packet.getZ());
		nearbyPlayers.put(packet.getEntityId(), loc);
	}

	public void removePlayers(ClientboundRemoveEntitiesPacket packet) {
		int[] entityIds = packet.getEntityIds();
		for (int i = 0; i < entityIds.length; i++) {
			nearbyPlayers.remove(entityIds[i]);
		}
	}

	public void updatePlayerPosition(ClientboundMoveEntityPosPacket packet) {
		Position loc = nearbyPlayers.get(packet.getEntityId());
		if (loc == null)
			return;

		loc.add(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());
	}

	public void updatePlayerPosition(ClientboundMoveEntityRotPacket packet) {
		Position loc = nearbyPlayers.get(packet.getEntityId());
		if (loc == null)
			return;

		loc.rot(packet.getYaw(), packet.getPitch());
	}

	public void updatePlayerPosition(ClientboundMoveEntityPosRotPacket packet) {
		Position loc = nearbyPlayers.get(packet.getEntityId());
		if (loc == null)
			return;

		loc.add(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ(), packet.getYaw(), packet.getPitch());
	}

	public void updatePlayerPosition(ClientboundTeleportEntityPacket packet) {
		Position loc = nearbyPlayers.get(packet.getEntityId());
		if (loc == null)
			return;

		loc.set(packet.getX(), packet.getY(), packet.getZ());
	}

	public Position getClosestPlayerLocation() {
		if (nearbyPlayers.isEmpty())
			return null;

		Position loc = bot.getPosition();
		Position closest = null;
		double leastDistance = MAX_DISTANCE;

		for (Position l : nearbyPlayers.values()) {
			double distance = loc.distance(l);
			if (distance < leastDistance && distance > 0.5) {
				leastDistance = loc.distance(l);
				closest = l;
			}
		}
		return closest;

	}

	public void clear() {
		nearbyPlayers.clear();
	}

	public void stop() {
		task.cancel();
	}

}
