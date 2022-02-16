package bot;

import auc.AuctionTracker;
import net.kyori.adventure.text.Component;

public class ChatReader {

	private MinecraftBot bot;
	private AuctionTracker tracker;

	public ChatReader(MinecraftBot bot) {
		this.bot = bot;
		this.tracker = new AuctionTracker();
	}

	public void readLine(Component message) {
		String line = getMessage(message);
		String[] words = line.split(" ");
		if (words[0].contentEquals("AUCTION"))
			trackAuc(words);
		else if (words[0].equalsIgnoreCase(bot.getUsername())) {
			readCommand(line, words);
		}
	}

	private String getMessage(Component message) {
		StringBuilder sb = new StringBuilder();
		sb.append(message.toString());
		for (Component child : message.children()) {
			sb.append(child.toString());
		}
		return sb.toString();
	}

	private void trackAuc(String[] words) {
		try {
			if (words[4].contentEquals("auctioning")) { // auction start
				short count = Short.parseShort(words[5]);
				StringBuilder name = new StringBuilder();
				for (int i = 6; i < words.length; i++) {
					if (words[i].contentEquals("for"))
						break;
					else
						name.append(words[i] + " ");
				}
				tracker.aucStart(name.substring(0, name.length() - 1), count);
			} else if (words[6].contentEquals("won")) { // auction finished
				String priceString = words[8].replaceAll(",", "");
				priceString = priceString.substring(1);
				System.out.println("DEBUG: price = " + priceString);
				long price = Long.parseLong(priceString);
				String buyer = words[10].replaceAll("!", "");
				tracker.aucEnd(price, buyer);
			}
		} catch (Exception e) {
			System.err.println("Failed to extract auction data from chat message");
			System.err.println(e.getMessage());
		}
	}

	private void readCommand(String line, String[] words) {
		if (line.contains(" price ") && (!line.contains("(From ") || !line.contains("§8(§aFrom"))) {
			String guess = line.substring(line.indexOf(" price ") + 7, line.length());
			String answer = tracker.getItemInfo(guess);
			System.out.println("Guess = " + guess);
			System.out.println("Anwer = " + answer);
			bot.sendControlledMessage(answer);
		}
	}

}
