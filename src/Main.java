import bot.MinecraftBot;

public class Main {
	
	public static void main(String[] args) {
		String username = System.getenv("mcusername");
		String password = System.getenv("password");
		MinecraftBot bot = new MinecraftBot(username, password);
		bot.login("survival.munchymc.com");
	}

}
