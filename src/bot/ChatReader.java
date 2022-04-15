package bot;

import auc.AuctionTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class ChatReader {

	private MinecraftBot bot;
	private AuctionTracker tracker;

	public ChatReader(MinecraftBot bot) {
		this.bot = bot;
		this.tracker = new AuctionTracker();
	}

	/**
	 * Analyzes 
	 * @param message
	 */
	public void readLine(Component message) {
		String line = fromComponent(message);
		System.out.println(line);
		String[] words = line.split(" ");
		if (words.length == 0)
			return;
		if (words[0].contentEquals("AUCTION"))
			trackAuc(words);
		else if (line.contains(bot.getUsername()) || line.contains("§8(§aFrom")) {
			readCommand(line, words);
		}
	}

	/**
	 * Converts Component to readable string
	 * @param message
	 * @return
	 */
	public String fromComponent(Component message) {
		StringBuilder sb = new StringBuilder();
		if (message instanceof TextComponent) {
			sb.append(((TextComponent) message).content());
			for (Component child : message.children()) {
				if (child instanceof TextComponent)
					sb.append(((TextComponent) child).content());
			}
		}
		return sb.toString();
	}

	/**
	 * Extracts information from auction messages and calls the AuctionTracker
	 * @param words
	 */
	private void trackAuc(String[] words) {
		try {
			if (words[4].contentEquals("auctioning")) { // auction start
				StringBuilder name = new StringBuilder();
				for (int i = 6; i < words.length; i++) {
					if (words[i].contentEquals("for") || words[i].contentEquals("|"))
						break;
					else
						name.append(words[i] + " ");
				}
				String item = name.substring(0, name.length() - 1);
				String buyer = words[2];
				short count = Short.parseShort(words[5]);
				tracker.aucStart(buyer, item, count);
			} else if (words[6].contentEquals("won")) { // auction finished
				String priceString = words[8].replaceAll(",", "").replaceAll("K", "000");
				priceString = priceString.substring(1);
				long price = Long.parseLong(priceString);
				String buyer = words[10].replaceAll("!", "");
				tracker.aucEnd(price, buyer);
			}
		} catch (Exception e) {
			System.err.println("Failed to extract auction data from chat message");
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Responds to chat commands such as "AucBot price [item]"
	 * @param line
	 * @param words
	 */
	private void readCommand(String line, String[] words) {
		if (line.contains(" price ") && (!line.contains("(From ") || !line.contains("§8(§aFrom"))) {
			String guess = line.substring(line.indexOf(" price ") + 7, line.length());
			String answer = tracker.getItemInfo(guess);
			bot.sendMessage(answer);
		}
	}

}
