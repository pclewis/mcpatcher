import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ElectricCart extends oc {
	static HashMap<Point, Integer> allPoweredPoints = new HashMap<Point,Integer>();
	int powerLevel = 0;

	private class Point {
		public int x, y, z;

		private Point(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public int distance(int x, int y, int z) {
			return Math.abs(this.x - x) + Math.abs(this.y - y) + Math.abs(this.z - z);
		}

		public boolean equals(Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;

			Point point = (Point) o;

			if(x != point.x) return false;
			if(y != point.y) return false;
			if(z != point.z) return false;

			return true;
		}

		public int hashCode() {
			int result = x;
			result = 31 * result + y;
			result = 31 * result + z;
			return result;
		}
	}

	List<Point> myPoweredPoints = new LinkedList<Point>();
	int lastX,lastY,lastZ;
	public ElectricCart(cn paramcn) {
		super(paramcn);
	}
	public ElectricCart(cn paramcn, double d1, double d2, double d3, int i) {
		super(paramcn, d1, d2, d3, i);
	}

	private boolean addPower(Point p) {
		if(powerLevel > 0 && !myPoweredPoints.contains(p)) {
			System.out.println("Adding power: " + p.x + "," + p.y + "," + p.z);
			myPoweredPoints.add(p);
			Integer i = allPoweredPoints.get(p);
			if(i==null) i = 0;
			allPoweredPoints.put(p, i+1);
			setPower(p, powerLevel);
			powerLevel -= 1;
			return true;
		}
		return false;
	}

	private void removePower(Point p, Iterator iter) {
		if(myPoweredPoints.contains(p)) {
			System.out.println("Removing power: " + p.x + "," + p.y + "," + p.z);
			if(iter != null)
				iter.remove();
			else
				myPoweredPoints.remove(p);
			Integer i = allPoweredPoints.get(p);
			assert(i != null);
			if(i.equals(1)) {
				allPoweredPoints.remove(p);
			} else {
				allPoweredPoints.put(p, i-1);
				setPower(p, 0);
			}
		}
	}

	private static boolean isMinecartPowered(Point p) {
		return allPoweredPoints.containsKey(p);
	}

	private void setPower(Point p, int level) {
		ag.b(p.x, p.y, p.z, level); // set level
		ly wire = ly.n[ag.a(p.x,p.y,p.z)]; // find instance
        if (wire != null) wire.a(ag, p.x, p.y, p.z, ly.aH.bc); // trigger update
	}

	private void setPowerLevel(Point m, Point p, int s) {
		if(Math.abs(this.an) < 0.001 && Math.abs(this.ap) < 0.001) {
			int trackType = ag.e(m.x,m.y,m.z);
			if(trackType == 0 && p.x==(m.x+1)) {
				this.ap = -0.5;
			} else if (trackType==0 && p.x==(m.x-1)) {
				this.ap = 0.5;
			} else if (trackType == 1 && p.z==(m.z+1)) {
				this.an = 0.5;
			} else if (trackType == 1 && p.z==(m.z-1)) {
				this.an = -0.5;
			} else if (trackType == 2) {
				this.ap = -0.5;
			} else if (trackType == 3) {
				this.ap = 0.5;
			} else if (trackType == 4) {
				this.an = 0.5;
			} else if (trackType == 5) {
				this.an = -0.5;
			} else {
				if(p.x==m.x) {
					this.ap = (m.z-p.z) * 0.5;
				} else {
					this.an = (m.x-p.x) * 0.5;
				}
			}
		}

		powerLevel = s;
	}

	private void cullPoweredPoints(int mx, int my, int mz, double speed) {Iterator<Point> i = myPoweredPoints.iterator();
		while(i.hasNext()) {
			Point p = i.next();
			int dist = p.distance(mx,my,mz);
			if(dist>2 || speed<0.001) {
				removePower(p,i);
			}
		}
	}

	private static final double SPEED_STEP = 0.05;
	private static double step(double from, double to) {
		if(from<0 && to>0)
			to=-to;
		if(from > to)
			return Math.max(to, from - SPEED_STEP);
		else
			return Math.min(to, from + SPEED_STEP);
	}

	private void setSpeed(double speed) {
		if(Math.abs(this.an) > 0.0001) {
			this.an = step(this.an, speed);
		}

		if(Math.abs(this.ap) > 0.0001) {
			this.ap = step(this.ap, speed);
		}
	}

	public void e_() {
		int mx = (int)Math.floor(this.ak), my = (int)Math.floor(this.al), mz = (int)Math.floor(this.am);
		double speed = (Math.abs(this.an) + Math.abs(this.ap));

		if(speed <= 0.001)
			powerLevel = 0;

		cullPoweredPoints(mx, my, mz, speed);

		for(int px = -1; px <= 1; ++px) {
			for(int pz = -1; pz <= 1; ++pz) {
				int x = mx + px;
				int y = my;
				int z = mz + pz;
				int dist = Math.abs(px) + Math.abs(pz);
				if(dist!=1)
					continue;
				Point p = new Point(x,y,z);
				if(ag.a(x,y,z) == ly.aw.bc) {
					int s = ag.e(x, y, z);
					if(s>0) {
						if(isMinecartPowered(p)) {
							addPower(p);
						} else {
							setPowerLevel(new Point(mx,my,mz), p, s);
						}
					} else {
						addPower(p);
					}
				}
			}
		}

		if(powerLevel > 0 && ag.a(mx,my,mz) == ly.aH.bc) {
			int trackType = ag.e(mx,my,mz);
			if(trackType == 0 || trackType == 1) {
				setSpeed(1.0 / (16-powerLevel));
			}
		}

		super.e_();
	}
}
