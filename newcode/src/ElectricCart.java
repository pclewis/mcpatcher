import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ElectricCart extends oc {
	private static final double SIN45 = Math.sin(Math.PI/4);
    private static final int OBSIDIAN_BLOCK = 0x31;
    private static final int WIRE_BLOCK     = 0x37;
	private static final int TRACK_BLOCK    = 0x42;

    private class PoweredPoint {
		public static final int TIMEOUT = 2;
		int refcount;
		int timeout;
		public PoweredPoint() {
			refcount = 0;
			timeout = TIMEOUT;
		}
	}
	private static HashMap<Point, PoweredPoint> allPoweredPoints = new HashMap<Point,PoweredPoint>();
	private List<Point> myPoweredPoints = new LinkedList<Point>();
	private int powerLevel = 0;
	private int takeOff = 0;
	private boolean isBraked = false;
	private boolean isInUpdate = false;
	private boolean hitEntity = false;
	private int dirX, dirZ;
	private int cooldown = 0;
	private int stopCounter = 0;

	private static final int COOLDOWN_TIME = 6;
	private static final int STOP_TIME = 3;
	private static final int TAKEOFF_TIME = 5;

	private static double[] SPEEDS = {
		Double.NaN,
		0.0, 0.2, 0.2, 0.2, 0.2,
		0.2, 0.2, 0.2, 0.2, 0.2,
		0.4, 0.4, 1.0, 1.0, 0.0
	};

	private static class Point {
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
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;

			Point point = (Point) o;

			if(x != point.x) return false;
			if(y != point.y) return false;
			if(z != point.z) return false;

			return true;
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
		if(powerLevel > 0) {
			if (!myPoweredPoints.contains(p)) {
				myPoweredPoints.add(p);
				PoweredPoint pp = allPoweredPoints.get(p);
				if(pp==null) {
					pp = new PoweredPoint();
					allPoweredPoints.put(p, pp);
				}
				pp.refcount += 1;
				pp.timeout = PoweredPoint.TIMEOUT;
			}
			setPower(p, powerLevel);
			return true;
		}
		return false;
	}

	private void removePower(Point p, Iterator iter, boolean force) {
		if(myPoweredPoints.contains(p)) {
			PoweredPoint pp = allPoweredPoints.get(p);
			if(pp.refcount==1) {
				if(pp.timeout>0 && !force) {
					//System.out.println(p + " timeout: " + pp.timeout);
					--pp.timeout;
					setPower(p, powerLevel);
					return;
				}
				allPoweredPoints.remove(p);
				setPower(p, 0);
			} else {
				pp.refcount -= 1;
			}

			if(iter != null)
				iter.remove();
			else
				myPoweredPoints.remove(p);
		}
	}

	public static boolean isMinecartPowered(Point p) {
		return allPoweredPoints.containsKey(p);
	}

	public static boolean isMinecartPowered(int x, int y, int z) {
		return isMinecartPowered(new Point(x,y,z));
	}

	private void setPower(Point p, int level) {
		if(ag.e(p.x,p.y,p.z) != level) {
			ag.b(p.x, p.y, p.z, level); // set level
			ly wire = ly.n[getBlockType(p.x, p.y, p.z)]; // find instance
			if (wire != null) wire.a(ag, p.x, p.y, p.z, TRACK_BLOCK); // trigger update
		}
	}

	private void setPowerLevel(Point m, Point p, int s) {
		double speed = SPEEDS[s];
		if(speed>0.0 && dirX==0 && dirZ==0) {
			takeOff = TAKEOFF_TIME;
			int trackType = getBlockSpecial(m.x, m.y, m.z);
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
		}

		powerLevel = s;
		//System.out.println("" + s + " power from " + p + " -- " + dirX + "," + dirZ);
	}

	private void cullPoweredPoints(int mx, int my, int mz) {
		Iterator<Point> i = myPoweredPoints.iterator();
		while(i.hasNext()) {
			Point p = i.next();
			int dist = p.distance(mx,p.y,mz);
			if(dist>1 || powerLevel<=0) {
				removePower(p, i, false);
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

		if((Math.abs(dirX) + Math.abs(dirZ)) > 1)
			speed = speed * SIN45;
		this.an = step(this.an, speed * dirX);
		this.ap = step(this.ap, speed * dirZ);

		//System.out.println("SET SPEED: " + this.an + "," + this.ap + " (" + this.ao + ")");
	}

	public void e_() {
		int mx = (int)Math.floor(this.ak), my = (int)Math.floor(this.al), mz = (int)Math.floor(this.am);
		boolean isOnTrack = getBlockType(mx, my, mz) == TRACK_BLOCK;
		if(!isOnTrack && getBlockType(mx, my-1, mz) == TRACK_BLOCK) {
			isOnTrack = true;
			my -= 1;
		}
		int trackType = isOnTrack ? getBlockSpecial(mx, my, mz) : -1;
		boolean isOnStraightTrack = (trackType == 0 || trackType == 1);

		if(takeOff > 0)
			--takeOff;

		cullPoweredPoints(mx, my, mz);

		if(isOnStraightTrack || isBraked) {
			LOOP: for(int px = -1; px <= 1; ++px) {
				for(int py = -1; py <= 0; ++py) {
					for(int pz = -1; pz <= 1; ++pz) {
						int x = mx + px;
						int y = my + py;
						int z = mz + pz;
						int dist = Math.abs(px) + Math.abs(pz);
						if(dist!=1)
							continue;
						if(getBlockType(x, y, z) == WIRE_BLOCK) {
							int s = getBlockSpecial(x, y, z);
							if(s>0 && (isBraked || cooldown <= 0 || SPEEDS[s] <= 0.001)) {
								if(isBraked) {
									isBraked = SPEEDS[s] <= 0.001;
								}
								setPowerLevel(new Point(mx,my,mz), new Point(x,y,z), s);
								if(takeOff == TAKEOFF_TIME)
									return;
								break LOOP;
							}
						}
					}
				}
			}
		}

		if(powerLevel>0) {
			for(int py = -1; py > -5; --py) {
				int bt = getBlockType(mx, my+py, mz);
                if(bt == OBSIDIAN_BLOCK)
                    break;
				if(bt == WIRE_BLOCK)
				    addPower(new Point(mx, my+py, mz));
			}
		}

		if(isBraked)
			return;

		if(powerLevel>0) {
			if(cooldown>0) {
				--cooldown;
			}
			if(cooldown<=2 && (isOnTrack)) {
				setSpeed(SPEEDS[powerLevel]);
			}
		}

		// make sure we don't take turns or hills too fast
		if(Math.abs(this.an) > 0.4 || Math.abs(this.ap) > 0.4) {
			double targX = 0.4 * Math.signum(this.an);
			double targZ = 0.4 * Math.signum(this.ap);

			if(!isOnStraightTrack) {
				if(Math.abs(this.an) > 0.4)
					this.an = targX;
				if(Math.abs(this.ap) > 0.4)
					this.ap = targZ;
			} else if (!nextTrackIsStraight()) {
				if(Math.abs(this.an) > 0.4)
					this.an = step(step(this.an, targX), targX);
				if(Math.abs(this.ap) > 0.4)
					this.ap = step(step(this.ap, targZ), targZ);
			}
		}

		isInUpdate = true;
		hitEntity = false;
		super.e_();
		isInUpdate = false;

		double distMoved = Math.abs(this.ah - this.ak) + Math.abs(this.aj - this.am);
		if(distMoved < 0.001) {
			if( SPEEDS[powerLevel] == 0 ) {
				isBraked = true;
				dirX = dirZ = 0;
			} else {
				if(++stopCounter > STOP_TIME) {
					dirX = dirZ = 0;
					powerLevel = 0;
				}
			}
		} else {
			stopCounter = 0;
			if(cooldown<=0) {
				updateDirection();
			}
		}
	}

	private boolean nextTrackIsStraight() {
		int mx = (int)Math.floor(this.ak + this.an),
			my = (int)Math.floor(this.al + this.ao),
			mz = (int)Math.floor(this.am + this.ap);
		boolean isOnTrack = getBlockType(mx, my, mz) == TRACK_BLOCK;
		if(!isOnTrack && getBlockType(mx, my-1, mz) == TRACK_BLOCK) {
			isOnTrack = true;
			my -= 1;
		}
		int trackType = isOnTrack ? getBlockSpecial(mx, my, mz) : -1;
		return (trackType == 0 || trackType == 1);
	}

	private int getBlockType(int mx, int my, int mz) {
		return ag.a(mx, my, mz);
	}

	private int getBlockSpecial(int mx, int my, int mz) {
		return ag.e(mx,my,mz);
	}

	private boolean isOnCorner() {
		int x=(int)this.ak, y=(int)this.al, z=(int)this.am;
		int s = getBlockSpecial(x,y,z);
		return getBlockType(x,y,z) == TRACK_BLOCK && s>=6 && s<=9;
	}

	private void updateDirection() {
		int newDirX = (int)Math.signum(this.ak - this.ah);
		int newDirZ = (int)Math.signum(this.am - this.aj);
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
        db.a("IsBraked", isBraked);
	}

	/* load */
	protected void b(hm db) {
		super.b(db);
		powerLevel = db.e("PowerLevel");
        isBraked = db.m("IsBraked");
	}

	/* destroyed */
	public void F() {
		Iterator<Point> i = myPoweredPoints.iterator();
		while(i.hasNext())
			removePower(i.next(), i, true);
		super.F();
	}

	private static double distance(double fx, double fy, double fz, double tx, double ty, double tz) {
		double x=(fx-tx), y=(fy-ty), z=(fz-tz);
		return Math.sqrt(x*x+y*y+z*z);
	}
	private boolean movingToward(kh e) {
		return Math.signum(an) == Math.signum(Math.floor(e.ak-ak))
			&& Math.signum(ap) == Math.signum(Math.floor(e.am-am));
	}

	/* collision */
	public void f(kh other) {
		if(other == this.ae) return; // ignore rider

		if(isInUpdate)
			hitEntity = true;

		if(!(other instanceof ElectricCart)) {
			if(powerLevel>0 && cooldown <= 0 && movingToward(other)) {
				cooldown = COOLDOWN_TIME;
			}
		} else {
			ElectricCart ec = (ElectricCart)other;

			// if another cart hit us but we're not moving, let them deal with it
			if(!isInUpdate) {
				if(ec.isInUpdate)
					other.f(this);
				return;
			}

			// motor is on and they're not coming towards us
			if(powerLevel>0 && cooldown <= 2 && !(ec.movingToward(this) && this.movingToward(ec))) {
				// we're moving the same speed or slower
				if (ec.powerLevel > 0 && SPEEDS[powerLevel] <= SPEEDS[ec.powerLevel]) {
					double dist = distance(ak,al,am,ec.ak,ec.al,ec.am);
					//System.out.println( "isOnCorner() " + isOnCorner() + "   ec.isOnCorner() " + ec.isOnCorner() );
					//System.out.println( "dist: " + dist );
					if ( dist > 1.00 ) {
						// far enough away, ignore the collision
						return;
					} else if (dist > 0.70) {
						// nudge them and slow down a little
						ec.an += (this.an * 0.10);
						ec.ap += (this.ap * 0.10);
						this.an *= 0.90;
						this.ap *= 0.90;
						return;
					}
				} else {
					//System.out.println("!!!!!!!! Bump !!!!!!!!!!!");
					cooldown = COOLDOWN_TIME;
				}
			}

			//System.out.println("Collision with cart  otherBraked = " + ec.isBraked + " me = " + this.isBraked );
			//System.out.println("this.movingToward(other) : " + this.movingToward(other));
			//System.out.println("other.movingToward(this) : " + ec.movingToward(this));
			// if we're taking off, just give them a chance to move first
			if(takeOff>0 && !ec.isInUpdate) {
				//System.out.println("Letting other guy update");
				ec.e_();
				return;
			} else if(ec.isBraked) {
				this.powerLevel = ec.powerLevel;
				double x=ec.an,z=ec.ap;
				super.f(other);
				ec.an=x; ec.ap=z;
				return;
			} else {
				// do normal resolve, but first transfer a bunch of energy to them
				ec.an += an*0.40;
				ec.ap += ap*0.40;
				this.an *= 0.40;
				this.ap *= 0.40;
			}
		}

		//System.out.println("Normal collision handler");
		super.f(other);
	}
}
