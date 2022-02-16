package bot;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;

import net.kyori.adventure.text.Component;

public class MinecraftBot {

	private String username;
	private String password;
	private AuthenticationService authService;
	private Session session;
	
	public MinecraftBot(String username, String password) {
		this.username = username;
		this.password = password;
		authenticate();
	}
	
	private void authenticate() {
		try {
			AuthenticationService authService = new MojangAuthenticationService();
			authService.setUsername(username);
			authService.setPassword(password);
			authService.login();
		} catch(Exception e) {
			System.err.println("Failed to authenticate bot");
		}
	}
	
	
	public void login(String host) {
		MinecraftProtocol protocol = new MinecraftProtocol(authService.getSelectedProfile(), authService.getAccessToken());
		SessionService sessionService = new SessionService();
		session = new TcpClientSession(host, 25565, protocol);
		session.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
		session.addListener(new SessionAdapter() {
			
			@Override
			public void packetReceived(Session session, Packet packet) {
				if (packet instanceof ClientboundLoginPacket) {
					System.out.println("Logged in succesfully");
				} else if (packet instanceof ClientboundChatPacket) {
					Component message = ((ClientboundChatPacket) packet).getMessage();
					System.out.println(message);
				}
			}

			@Override
			public void disconnected(DisconnectedEvent event) {
				System.out.println("Disconnected: " + event.getReason());
				if (event.getCause() != null) {
					event.getCause().printStackTrace();
				}
			}
		});

		session.connect();
	}
	
	public void sendMessage(String message) {
		session.send(new ServerboundChatPacket(message));
	}

}
