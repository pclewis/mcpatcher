/**
 * Created by MrMessiah
 */
public class BetterGrass extends my {
	public BetterGrass(int i) {
		super(i);
		this.bb = 0;
	}

	public int a(nm nm1, int i, int j, int k, int l) {
		if(l == 0) return 2;
		gb gb1 = nm1.f(i, j + 1, k);
		if((gb1 == gb.s) || (gb1 == gb.t)) {
			if((l == 2) && ((nm1.f(i, j, k - 1) == gb.s) || (nm1.f(i, j, k - 1) == gb.t))) return 66;
			if((l == 3) && ((nm1.f(i, j, k + 1) == gb.s) || (nm1.f(i, j, k + 1) == gb.t))) return 66;
			if((l == 4) && ((nm1.f(i - 1, j, k) == gb.s) || (nm1.f(i - 1, j, k) == gb.t))) return 66;
			if((l == 5) && ((nm1.f(i + 1, j, k) == gb.s) || (nm1.f(i + 1, j, k) == gb.t))) return 66;
			return 68;
		}
		if((l == 2) && (nm1.a(i, j - 1, k - 1) != ly.v.bc)) return 3;
		if((l == 3) && (nm1.a(i, j - 1, k + 1) != ly.v.bc)) return 3;
		if((l == 4) && (nm1.a(i - 1, j - 1, k) != ly.v.bc)) return 3;
		if((l == 5) && (nm1.a(i + 1, j - 1, k) != ly.v.bc)) return 3;

		return 0;
	}
}
