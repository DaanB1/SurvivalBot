package bot;

public class Position {

	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;

	public Position(double x, double y, double z) {
		this(x, y, z, 0, 0);
	}
	
	public Position(double x, double y, double z, float yaw, float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}
	
	public float getYaw() {
		return yaw;
	}
	
	public float getPitch() {
		return pitch;
	}

	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
	}
	
	public void add(double x, double y, double z, float yaw, float pitch) {
		this.x += x;
		this.y += y;
		this.z += z;
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	public void rot(float yaw, float pitch) {
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public double distance(Position other) {
		double dx = x - other.getX();
		double dy = y - other.getY();
		double dz = z - other.getZ();
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public String toString() {
		return "(X: " + x + ", Y: " + y + ", Z: " + z + ")";
	}

}
