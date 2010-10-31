/**
 * Created by MrMessiah
 */
public class BetterGrass extends of {
	public BetterGrass(int i) {
		super(i);
		this.bh = 0;
	}

    @Override
	public int a(ox ox1, int i, int j, int k, int l) {
		if(l == 0) return 2;
		gt gt1 = ox1.f(i, j + 1, k);
		if((gt1 == gt.s) || (gt1 == gt.t)) {
			if((l == 2) && ((ox1.f(i, j, k - 1) == gt.s) || (ox1.f(i, j, k - 1) == gt.t))) return 66;
			if((l == 3) && ((ox1.f(i, j, k + 1) == gt.s) || (ox1.f(i, j, k + 1) == gt.t))) return 66;
			if((l == 4) && ((ox1.f(i - 1, j, k) == gt.s) || (ox1.f(i - 1, j, k) == gt.t))) return 66;
			if((l == 5) && ((ox1.f(i + 1, j, k) == gt.s) || (ox1.f(i + 1, j, k) == gt.t))) return 66;
			return 68;
		}
		if((l == 2) && (ox1.a(i, j - 1, k - 1) != ne.v.bi)) return 3;
		if((l == 3) && (ox1.a(i, j - 1, k + 1) != ne.v.bi)) return 3;
		if((l == 4) && (ox1.a(i - 1, j - 1, k) != ne.v.bi)) return 3;
		if((l == 5) && (ox1.a(i + 1, j - 1, k) != ne.v.bi)) return 3;

		return 0;
	}
}
