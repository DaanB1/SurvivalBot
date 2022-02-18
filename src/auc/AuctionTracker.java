package auc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;

public class AuctionTracker {

	private Database db;
	private ArrayList<String> knownItems;
	private String seller;
	private String item;
	private short count;

	public AuctionTracker() {
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

	public String getItemInfo(String itemGuess) {
		String item = bestGuess(itemGuess);
		if (item == null)
			return "Could not find any information about '" + itemGuess + "'";

		long totalPrice = 0;
		long recentPrice = 0;
		int totalCount = 0;
		int recentCount = 0;
		boolean stackable = false;

		Instant now = Instant.now();
		// retrieve data about item from database
		try {
			String querry = "SELECT count, price, datetime FROM survauc WHERE item = '" + item + "'";
			ResultSet rs = db.getStatement().executeQuery(querry);
			while (rs.next()) {
				short count = rs.getShort("count");
				long price = rs.getLong("price");
				Timestamp then = rs.getTimestamp("datetime");

				totalPrice += price;
				totalCount += count;
				if (ChronoUnit.DAYS.between(now, then.toInstant()) <= 14) {
					recentPrice += price;
					recentCount += count;
				}
				if (count > 1) {
					stackable = true;
				}
			}
		} catch (SQLException e) {
			// item does not exist in database
			System.out.println(e.getMessage());
			return "Could not find any information about '" + item + "'";
		}

		// calculate simple statistics from data
		double totalAvg = totalPrice / (double) totalCount;
	
		if (stackable) {
			System.out.println("DEBUG: " + totalAvg + ", " + totalAvg * 64 + ", " + (int) (totalAvg * 64));
			System.out.println("DEBUG: " + totalPrice + ", " + totalCount);
			return "64 " + item + " sells on average for " + (int) (totalAvg * 64) + "g (" + totalCount + " total sales)";
		} else
			return "1 " + item + " sells on average for " + (int) totalAvg + "g (" + totalCount + " total sales)";
	}

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
			int dist = levenstein(itemGuess, item.toLowerCase());
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

	private static int levenstein(String x, String y) {
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
		return a == b ? 0 : 1;
	}

	private static int min(int... numbers) {
		return Arrays.stream(numbers).min().orElse(Integer.MAX_VALUE);
	}

}
