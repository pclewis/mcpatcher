import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ElectricCart extends oc {
	private static HashMap<Point, Integer> allPoweredPoints = new HashMap<Point,Integer>();
	private List<Point> myPoweredPoints = new LinkedList<Point>();
	private boolean blockedDirections[][] = new boolean[3][3];
	private int powerLevel = 0;
	private boolean isBraked = false;
	private boolean isInUpdate = false;
	private boolean hitEntity = false;
	private int dirX, dirZ;
	private int cooldown = 0;
	private int stopCountdown = 0;

	private static final int COOLDOWN_TIME = 4;
	private static final int STOPPED_TIME = 3;

	private static double[] SPEEDS = {
		Double.NaN,
		0.0, 0.2, 0.2, 0.2, 0.2,
		0.2, 0.2, 0.2, 0.2, 0.2,
		0.4, 0.4, 1.0, 1.0, 0.0
	};

	private class Point {
		public String toString() {
			return "{" + x + ", " + y + ", " + z + '}';
		}

		public final int x, y, z, hashCode;

		private Point(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.hashCode = x|(y<<10)|(z<<20);
		}

		public int distance(int x, int y, int z) {
			int dx = x - this.x;
			int dy = y - this.y;
			int dz = z - this.z;
			return (int)Math.sqrt((dx*dx)+(dy*dy)+(dz*dz));
		}

		public boolean equals(Object o) {
			return ((Point)o).hashCode == this.hashCode;
		}

		public int hashCode() {
			return hashCode;
		}
	}

	public ElectricCart(cn paramcn) {
		super(paramcn);
	}
	public ElectricCart(cn paramcn, double d1, double d2, double d3, int i) {
		super(paramcn, d1, d2, d3, i);
	}

	private boolean addPower(Point p) {
		if(powerLevel > 0 && !myPoweredPoints.contains(p)) {
			//System.out.println("Adding power: " + p);
			myPoweredPoints.add(p);
			Integer i = allPoweredPoints.get(p);
			if(i==null) i = 0;
			allPoweredPoints.put(p, i+1);
			//System.out.println("Refcount: " + (i+1));
			setPower(p, powerLevel);
			//powerLevel -= 1;
			return true;
		}
		return false;
	}

	private void removePower(Point p, Iterator iter) {
		if(myPoweredPoints.contains(p)) {
			//System.out.println("Removing power: " + p);
			if(iter != null)
				iter.remove();
			else
				myPoweredPoints.remove(p);
			Integer i = allPoweredPoints.get(p);
			assert(i != null);
			//System.out.println("Refcount: " + i);
			if(i.equals(1)) {
				allPoweredPoints.remove(p);
				setPower(p, 0);
			} else {
				allPoweredPoints.put(p, i-1);
			}
		}
	}

	private static boolean isMinecartPowered(Point p) {
		return allPoweredPoints.containsKey(p);
	}

	private void setPower(Point p, int level) {
		if(ag.e(p.x,p.y,p.z) != level) {
			//System.out.println("Set power at " + p + " to " + level);
			ag.b(p.x, p.y, p.z, level); // set level
			ly wire = ly.n[ag.a(p.x,p.y,p.z)]; // find instance
			if (wire != null) wire.a(ag, p.x, p.y, p.z, ly.aH.bc); // trigger update
		}
	}

	private void setPowerLevel(Point m, Point p, int s) {
		double speed = SPEEDS[s];
		if(speed>0.0 && Math.abs(this.an) < 0.001 && Math.abs(this.ap) < 0.001) {
			int trackType = ag.e(m.x,m.y,m.z);
			if(trackType == 0 && p.x==(m.x+1)) {
				dirZ = -1;
			} else if (trackType==0 && p.x==(m.x-1)) {
				dirZ = 1;
			} else if (trackType == 1 && p.z==(m.z+1)) {
				dirX = 1;
			} else if (trackType == 1 && p.z==(m.z-1)) {
				dirX = -1;
			} else if (trackType == 2) {
				dirZ = -1;
			} else if (trackType == 3) {
				dirZ = 1;
			} else if (trackType == 4) {
				dirX = 1;
			} else if (trackType == 5) {
				dirX = -1;
			} else {
				if(p.x==m.x) {
					dirZ = m.z>p.z ? 1 : -1;
				} else {
					dirX = m.x>p.x ? 1 : -1;
				}
			}
		} else if (speed>0.0) {
			dirX = dirZ = 0;
			updateDirection();
		}

		powerLevel = s;
		//System.out.println("" + s + " power from " + p);
	}

	private void cullPoweredPoints(int mx, int my, int mz) {
		Iterator<Point> i = myPoweredPoints.iterator();
		while(i.hasNext()) {
			Point p = i.next();
			int dist = p.distance(mx,p.y,mz);
			if(dist>1 || powerLevel<=0) {
				removePower(p,i);
			}
		}
	}

	private static final double SPEED_STEP = 0.025;
	private static double step(double from, double to) {
		double result = 0;
		if(from > to)
			result = Math.max(to, from - SPEED_STEP);
		else
			result = Math.min(to, from + SPEED_STEP);
		//System.out.println("step("+from + ","+to+") -> " + result);
		return result;
	}

	private void setSpeed(double speed) {
		if(speed <= 0.001) { // brakes
			this.an = step(this.an, 0);
			this.ap = step(this.ap, 0);
			return;
		}

		this.an = step(this.an, speed * dirX);
		this.ap = step(this.ap, speed * dirZ);

		//System.out.println("SET SPEED: " + this.an + "," + this.ap + " (" + this.ao + ")");
	}

	public void e_() {
		int mx = (int)Math.floor(this.ak), my = (int)Math.floor(this.al), mz = (int)Math.floor(this.am);
		boolean isOnTrack = ag.a(mx, my, mz) == ly.aH.bc;
		if(!isOnTrack && ag.a(mx,my-1,mz) == ly.aH.bc) {
			isOnTrack = true;
			my -= 1;
		}
		int trackType = isOnTrack ? ag.e(mx,my,mz) : -1;
		boolean isOnStraightTrack = (trackType == 0 || trackType == 1);
		boolean isOnRamp = (trackType >= 2 && trackType <= 5);

		//System.out.println("("+mx+","+my+","+mz+") onTrack:"+ isOnTrack+"  ramp: " + isOnRamp);

		if(powerLevel>0) {
			// if stopped but not braking, kill motor
			double speed = (Math.abs(this.an) + Math.abs(this.ap));
			if(speed < (SPEED_STEP/4)) {
				if (SPEEDS[powerLevel] > 0) {
					/*
					if(++stopCountdown >= STOPPED_TIME) {
						stopCountdown = 0;
						powerLevel = 0;
					}
					*/
				} else {
					isBraked = true;
				}
			} else if(cooldown<=0){
				updateDirection();
				stopCountdown = 0;
			}
		}

		cullPoweredPoints(mx, my, mz);

		if(isOnStraightTrack) {
			for(int px = -1; px <= 1; ++px) {
				for(int py = -1; py <= 0; ++py) {
					for(int pz = -1; pz <= 1; ++pz) {
						int x = mx + px;
						int y = my + py;
						int z = mz + pz;
						int dist = Math.abs(px) + Math.abs(pz);
						if(dist!=1)
							continue;
						if(ag.a(x,y,z) == ly.aw.bc) {
							int s = ag.e(x, y, z);
							if(s>0) {
								if(isBraked) {
									isBraked = SPEEDS[s] <= 0.001;
								}
								setPowerLevel(new Point(mx,my,mz), new Point(x,y,z), s);
							}
						}
					}
				}
			}
		}

		if(powerLevel>0) {
			for(int py = -2; py > -5; --py) {
				int bt = ag.a(mx,my+py,mz);
				if(bt != ly.aw.bc) {
					continue;
				}
				addPower(new Point(mx, my+py, mz));
			}
		}

		if(isBraked)
			return;

		if(powerLevel>0) {
			if(cooldown>0) {
				if(--cooldown==0)
					this.an = this.ap = 0;
			}
			if(cooldown==0 && (isOnTrack)) {
				setSpeed(SPEEDS[powerLevel]);
			}
		}

		isInUpdate = (this.an!=0.0D) || (this.ap!=0.0D);
		int tryX = (int)Math.signum(this.an), tryZ = (int)Math.signum(this.ap);
		hitEntity = false;

		super.e_();

		if(isInUpdate) {
			/* tried to move and didn't, probably ran into a wall */
			if(!hitEntity && this.an==0.0D && this.ap==0.0D) {
				System.out.println("Stopped by something");
				if(SPEEDS[powerLevel]==0)
					isBraked = true;
				else {
					powerLevel = 0;
					blockedDirections[tryX+1][tryZ+1] = true;
					System.out.println("Block: " + tryX + "," + tryZ);
				}
			} else if(this.an!=0.0D || this.ap!=0.0D) {
				blockedDirections[tryX+1][tryZ+1] = false;
				//System.out.println("Unblock: " + tryX + "," + tryZ);
			}
		}

		isInUpdate = false;
	}

	private void updateDirection() {
		int newDirX = (int)Math.signum(this.an);
		int newDirZ = (int)Math.signum(this.ap);
		if((dirX != 0 && newDirX == -dirX) || (dirZ != 0 && newDirZ == -dirZ)) // no 180s allowed
			return;
		dirX = newDirX;
		dirZ = newDirZ;
		//System.out.println("updated direction: (" + dirX + "," + dirZ + ")");
	}

	/* save */
	protected void a(hm db) {
		super.a(db);
		db.a("PowerLevel", powerLevel);
	}

	/* load */
	protected void b(hm db) {
		super.b(db);
		powerLevel = db.e("PowerLevel");
	}

	/* destroyed */
	public void F() {
		Iterator<Point> i = myPoweredPoints.iterator();
		while(i.hasNext())
			removePower(i.next(), i);
		super.F();
	}

	private static double distance(double fx, double fy, double fz, double tx, double ty, double tz) {
		double x=(fx-tx), y=(fy-ty), z=(fz-tz);
		return Math.sqrt(x*x+y*y+z*z);
	}
	private boolean movingToward(kh e) {
		// see if past distance is further than current distance
		// seeing if future dist is farther would not work if our speed is enough to pass entirely through
		return distance(ak-an, al-ao, ak-ap, e.ak, e.al, e.am)
		     > distance(ak, al, am, e.ak, e.al, e.am);
	}

	/* collision */
	public void f(kh other) {
		if(other == this.ae) return; // ignore rider

		if(isInUpdate)
			hitEntity = true;

		// if another cart hit us but we're not moving, let them deal with it
		if(other instanceof ElectricCart && !isInUpdate) {
			if(((ElectricCart)other).isInUpdate)
				other.f(this);
			return;
		}

		if(powerLevel>0 && cooldown==0 && movingToward(other)) {
			cooldown = COOLDOWN_TIME;
		}
		
		if(other instanceof ElectricCart) {
			ElectricCart ec = (ElectricCart)other;
			//System.out.println("Collision with cart  otherBraked = " + ec.isBraked + " me = " + this.isBraked );

			if(ec.isBraked) {
				this.powerLevel = ec.powerLevel;
				double x=ec.an,z=ec.ap;
				super.f(other);
				ec.an=x; ec.ap=z;
				return;
			} else if (ec.blockedDirections[dirX+1][dirZ+1]) {
				this.powerLevel = 0;
				this.blockedDirections[dirX+1][dirZ+1] = true;
				System.out.println("Chain block: " + dirX + "," + dirZ);
			} else {
				// do normal resolve, but first transfer a bunch of energy to them
				ec.an += an*0.50;
				ec.ap += ap*0.50;
				this.an *= 0.75;
				this.ap *= 0.75;
			}
		}

		super.f(other);
	}
}
