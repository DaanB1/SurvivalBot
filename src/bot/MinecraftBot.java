package bot;

import java.util.UUID;

import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.ResourcePackStatus;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundResourcePackPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundPongPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundResourcePackPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;

public class MinecraftBot {

	private String username;
	private String password;
	private String host;

	private AccountType type;
	private AuthenticationService authService;
	private Session session;

	private ControlledMessageSender cms;
	private AutoReconnect ar;
	private ChatReader cr;
	private PlayerTracker pt;

	private Position loc;
	private int entityId;
	private double pitch = 0;
	private double yaw = 0;
	private boolean isSneaking = false;

	/**
	 * Constructor for online accounts
	 * 
	 * @param username
	 * @param password
	 * @param type
	 */
	public MinecraftBot(String username, String password, AccountType type) {
		this.username = username;
		this.password = password;
		this.type = type;
		this.cms = new ControlledMessageSender(this);
		this.ar = new AutoReconnect(this);
		this.cr = new ChatReader(this);
		this.pt = new PlayerTracker(this);
		authenticate();
	}

	public enum AccountType {
		MOJANG, MICROSOFT
	}

	/**
	 * Logs in on Mojang / Microsoft account.
	 */
	public void authenticate() {
		System.out.println("Authenticating bot...");
		authService = type == AccountType.MOJANG ? new MojangAuthenticationService()
				: new MsaAuthenticationService(UUID.randomUUID().toString());
		try {
			authService.setUsername(username);
			authService.setPassword(password);
			authService.login();
			System.out.println("Successfully authenticated bot");
		} catch (Exception e) {
			System.err.println("Failed to authenticate bot");
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Joins the server. Bot must have been authenticated for this to work.
	 * 
	 * @param host : the server ip
	 */
	public void login(String host) {
		System.out.println("Joining " + host + "...");
		this.host = host;
		MinecraftProtocol protocol = new MinecraftProtocol(authService.getSelectedProfile(),
				authService.getAccessToken());
		SessionService sessionService = new SessionService();
		session = new TcpClientSession(host, 25565, protocol);
		session.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
		session.addListener(new SessionAdapter() {

			@Override
			public void packetReceived(Session session, Packet packet) {

				// Packets used for logging in
				if (packet instanceof ClientboundLoginPacket) {
					entityId = ((ClientboundLoginPacket) packet).getEntityId();
					System.out.println(getUsername() + " logged in succesfully");
					pt.start();

				} else if (packet instanceof ClientboundKeepAlivePacket) {
					long pingId = ((ClientboundKeepAlivePacket) packet).getPingId();
					session.send(new ServerboundKeepAlivePacket(pingId));

				} else if (packet instanceof ClientboundPingPacket) {
					int pingId = ((ClientboundPingPacket) packet).getId();
					session.send(new ServerboundPongPacket(pingId));

				} else if (packet instanceof ClientboundResourcePackPacket) {
					session.send(new ServerboundResourcePackPacket(ResourcePackStatus.ACCEPTED));
					session.send(new ServerboundResourcePackPacket(ResourcePackStatus.SUCCESSFULLY_LOADED));

				} else if (packet instanceof ClientboundDisconnectPacket) {
					String reason = cr.fromComponent(((ClientboundDisconnectPacket) packet).getReason());
					System.err.println("Disconnected: " + reason);
					pt.stop();
					ar.reconnect();

					// Packets used for tracking players
				} else if (packet instanceof ClientboundPlayerPositionPacket) {
					ClientboundPlayerPositionPacket p = (ClientboundPlayerPositionPacket) packet;
					if (loc == null)
						loc = new Position(p.getX(), p.getY(), p.getZ());
					else
						loc.set(p.getX(), p.getY(), p.getZ());
					session.send(new ServerboundMovePlayerPosPacket(true, loc.getX(), loc.getY(), loc.getZ()));

				} else if (packet instanceof ClientboundMoveEntityPosRotPacket) {
					pt.updatePlayerPosition((ClientboundMoveEntityPosRotPacket) packet);

				} else if (packet instanceof ClientboundMoveEntityPosPacket) {
					pt.updatePlayerPosition((ClientboundMoveEntityPosPacket) packet);

				} else if (packet instanceof ClientboundMoveEntityRotPacket) {
					pt.updatePlayerPosition((ClientboundMoveEntityRotPacket) packet);

				} else if (packet instanceof ClientboundTeleportEntityPacket) {
					pt.updatePlayerPosition((ClientboundTeleportEntityPacket) packet);

				} else if (packet instanceof ClientboundAddPlayerPacket) {
					pt.addPlayer((ClientboundAddPlayerPacket) packet);

				} else if (packet instanceof ClientboundRemoveEntitiesPacket) {
					pt.removePlayers((ClientboundRemoveEntitiesPacket) packet);

					// Packets used for chat
				} else if (packet instanceof ClientboundChatPacket) {
					cr.readLine(((ClientboundChatPacket) packet).getMessage());
				}
			}

			@Override
			public void disconnected(DisconnectedEvent event) {
				System.err.println("Disconnected: " + event.getCause().getMessage());
				pt.stop();
				ar.reconnect();
			}
		});

		session.connect();
	}

	/**
	 * Instantly sends a message to the server, bypassing the internal cooldown
	 * 
	 * @param message
	 */
	public void sendInstantMessage(String message) {
		session.send(new ServerboundChatPacket(message));
	}

	/**
	 * Queues up messages and sends them out using an internal cooldown to prevent
	 * spam
	 * 
	 * @param message
	 */
	public void sendMessage(String message) {
		cms.sendMessage(message);
	}

	/**
	 * Rotates the bot according to the yaw and pitch
	 * 
	 * @param yaw
	 * @param pitch
	 */
	public void rotate(float yaw, float pitch) {
		if (this.yaw == yaw && this.pitch == pitch)
			return;
		session.send(new ServerboundMovePlayerRotPacket(true, yaw, pitch));
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public void startSneaking() {
		if (!isSneaking) {
			session.send(new ServerboundPlayerCommandPacket(entityId, PlayerState.START_SNEAKING));
			isSneaking = true;
		}
	}

	public void stopSneaking() {
		if (isSneaking) {
			session.send(new ServerboundPlayerCommandPacket(entityId, PlayerState.STOP_SNEAKING));
			isSneaking = false;
		}
	}

	public String getUsername() {
		return authService.getSelectedProfile().getName();
	}

	public String getHost() {
		return host;
	}

	public Position getPosition() {
		return loc;
	}

	public int getEntityId() {
		return entityId;
	}

}