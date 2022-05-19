package bot;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;

//Tracks the location of players nearby
public class PlayerTracker {

	private MinecraftBot bot;
	private HashMap<Integer, Position> nearbyPlayers;
	private HashMap<UUID, PlayerListEntry> playerListEntries;

	public PlayerTracker(MinecraftBot bot) {
		this.bot = bot;
		this.nearbyPlayers = new HashMap<>();
	}
	
	public void updatePlayerList(ClientboundPlayerInfoPacket packet) {
		PlayerListEntry[] entries = packet.getEntries();
		for(int i = 0; i < entries.length; i++) {
			switch(packet.getAction()) {
			case ADD_PLAYER:
				playerListEntries.put(entries[i].getProfile().getId(), entries[i]);
				System.out.println("++[" + entries[i].getProfile().getName() + "]");
				break;
			case REMOVE_PLAYER:
				playerListEntries.remove(entries[i].getProfile().getId());
				System.out.println("--[" + entries[i].getProfile().getName() + "]");
				break;
			default:
				return;
			}
		}
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
		double leastDistance = Double.MAX_VALUE;

		for (Position l : nearbyPlayers.values()) {
			double distance = loc.distance(l);
			if (distance < leastDistance && distance > 0.5) {
				leastDistance = loc.distance(l);
				closest = l;
			}
		}
		return closest;
	}
	
	public Collection<Position> getAllPositions() {
		return nearbyPlayers.values();
	}
	
	public PlayerListEntry getPlayerInfo(UUID id) {
		return playerListEntries.get(id);
	}

}
