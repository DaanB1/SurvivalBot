package auc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class AuctionTracker {

	private HashMap<String, Instant> requests;
	private static final int REQUEST_COOLDOWN = 60;
	
	private Database db;
	private ArrayList<String> knownItems;
	private String seller;
	private String item;
	private short count;

	public AuctionTracker() {
		requests = new HashMap<>();
		item = null;
		count = 0;
		knownItems = new ArrayList<>();
		db = new Database();

		try {
			ResultSet rs = db.getStatement().executeQuery("SELECT DISTINCT item FROM survauc;");
			while (rs.next()) {
				knownItems.add(rs.getString("item").stripTrailing());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void aucStart(String seller, String item, short count) {
		this.seller = seller;
		this.item = item.replaceAll("'", "");
		this.count = count;
	}

	public void aucEnd(long price, String buyer) {
		if (item != null) {
			Timestamp datetime = Timestamp.from(Instant.now());
			String querry = "INSERT INTO survauc VALUES ('" + item + "'," + count + "," + price + ",'" + seller + "','"
					+ buyer + "','" + datetime + "')";
			try {
				db.getStatement().executeUpdate(querry);
			} catch (SQLException e) {
				System.err.println("Failed to update database");
				System.err.println(e.getMessage());
			}
			if (!knownItems.contains(item))
				knownItems.add(item);
			item = null;
			count = 0;
		}
	}

	/**
	 * Retrieves information about the specified item and returns it in a formatted string.
	 * @param itemGuess
	 * @return
	 */
	public String getItemInfo(String itemGuess, String requestedBy) {
		requests.put(requestedBy, Instant.now());
		String item = bestGuess(itemGuess);
		if (item == null)
			return "Could not find any information about that item";

		long totalPrice = 0;
		int totalCount = 0;
		boolean stackable = false;

		// retrieve data about item from database
		try {
			String querry = "SELECT count, price, datetime FROM survauc WHERE item = '" + item + "'";
			ResultSet rs = db.getStatement().executeQuery(querry);
			while (rs.next()) {
				short count = rs.getShort("count");
				long price = rs.getLong("price");
				totalPrice += price;
				totalCount += count;
				if (count > 1) {
					stackable = true;
				}
			}
		} catch (SQLException e) {
			// item does not exist in database
			System.out.println(e.getMessage());
			return "Could not find any information about that item";
		}

		// calculate simple statistics from data
		double totalAvg = totalPrice / (double) totalCount;

		if (stackable && totalAvg < 3) {
			return "64 " + item + " sells on average for $" + (int) (totalAvg * 64) + " (" + totalCount
					+ " total sales)";
		} else
			return "1 " + item + " sells on average for $" + (int) totalAvg + " (" + totalCount + " total sales)";
	}
	
	/**
	 * Checks if player can make a new item lookup request
	 * @param ign
	 * @return
	 */
	public boolean canRequest(String ign) {
		if(!requests.containsKey(ign))
			return true;
		
		if(Duration.between(requests.get(ign), Instant.now()).getSeconds() < REQUEST_COOLDOWN) {
			return false;
		}
		return true;
	}
	
	/**
	 * Gets the amount of seconds a player has to wait before being able to make a new request
	 * @param ign
	 * @return
	 */
	public int getCooldown(String ign) {
		if(!requests.containsKey(ign))
			return 0;
		
		return REQUEST_COOLDOWN - (int) Duration.between(requests.get(ign), Instant.now()).getSeconds();
	}

	/**
	 * Attempts to determine which items is meant. 
	 * eg: bestGuess("gold igot") returns "GOLD_INGOT"
	 * returns null when no match is found.
	 * 
	 * @param itemGuess
	 * @return
	 */
	private String bestGuess(String itemGuess) {
		itemGuess = itemGuess.toLowerCase();
		if (knownItems.contains(itemGuess)) {
			return itemGuess;
		}
		String bestGuess = "";
		int leastDist = Integer.MAX_VALUE;
		for (String item : knownItems) {
			if (!item.toLowerCase().startsWith(itemGuess.substring(0, 1)))
				continue;
			int dist = levenshtein(itemGuess, item.toLowerCase());
			if (dist < leastDist) {
				leastDist = dist;
				bestGuess = item;
			}
		}
		if (!bestGuess.contentEquals("") && (double) leastDist / (double) bestGuess.length() < 0.4) {
			return bestGuess;
		} else {
			return null;
		}
	}

	// credits to stackoverflow for the part below
	private static int levenshtein(String x, String y) {
		int[][] dp = new int[x.length() + 1][y.length() + 1];

		for (int i = 0; i <= x.length(); i++) {
			for (int j = 0; j <= y.length(); j++) {
				if (i == 0) {
					dp[i][j] = j;
				} else if (j == 0) {
					dp[i][j] = i;
				} else {
					dp[i][j] = min(dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
							dp[i - 1][j] + 1, dp[i][j - 1] + 1);
				}
			}
		}
		return dp[x.length()][y.length()];
	}

	private static int costOfSubstitution(char a, char b) {
		if (a == ' ' || b == ' ' || a == '_' || b == '_')
			return 0;
		return a == b ? 0 : 1;
	}

	private static int min(int... numbers) {
		return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
	}

}
