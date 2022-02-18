import bot.MinecraftBot;

public class Main {
	
	public static void main(String[] args) {
		//String username = System.getenv("username");
		//String password = System.getenv("password");
		String username = "even.johansen.2005@gmail.com";
		String password = "https://i.imgur.com/IgBb8Nw.png";
		MinecraftBot bot = new MinecraftBot(username, password);
		bot.login("survival.munchymc.com");
	}

}
