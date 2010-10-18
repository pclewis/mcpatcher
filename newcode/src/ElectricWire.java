public class ElectricWire extends kf {
	private static int skipX, skipY = 9999, skipZ;

	public ElectricWire(int i, int i1) {
		super(i, i1);
	}

	protected void h(cn state, int x, int y, int z) {
		//System.out.println(String.format("h(%d,%d,%d)", x, y, z));
		if(x==skipX&&y==skipY&&z==skipZ) {
			//System.out.println("Skip");
			//super.i(state, x, y, z);
		} else {
			super.h(state, x, y, z); // must patch minecraft.jar compiling against
		}
	}

	public void a(cn state, int x, int y, int z, int src) {
		//System.out.println(String.format("a(%d,%d,%d,%d)", x, y, z, src));
		if(x==skipX&&y==skipY&&z==skipZ) {
			//System.out.println("SkipA");
		} else if(src == ly.aH.bc && state.e(x,y,z)>0) { // track piece
			//System.out.println("Updated by track piece");
			skipX=x;skipY=y;skipZ=z;
			state.g(x,y,z,this.bc);
			state.b(x,y,z,x,y,z);
			state.g(x-1,y,z,this.bc);
			state.g(x+1,y,z,this.bc);
			state.g(x,y-1,z,this.bc);
			state.g(x,y+1,z,this.bc);
			state.g(x,y,z-1,this.bc);
			state.g(x,y,z+1,this.bc);
			skipY=9999;
		} else {
			//System.out.println("Updated by other.");
			super.a(state, x, y, z, src);
		}
	}

	/*
	public void b(cn state, int x, int y, int z) {
		//System.out.println(String.format("b(%d,%d,%d)", x, y, z));
		super.b(state, x, y, z);
	}

	public void e(cn state, int x, int y, int z) {
		//System.out.println(String.format("e(%d,%d,%d)", x, y, z));
		super.e(state, x, y, z);
	}
	*/
}
