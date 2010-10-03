import javassist.bytecode.ConstPool;
import javassist.bytecode.Mnemonic;
import javassist.bytecode.Opcode;

class Patches implements Opcode {
	public static ParamSpec[] PSPEC_EMPTY = new ParamSpec[]{};
	public static ParamSpec[] PSPEC_TILESIZE = new ParamSpec[]{
		new ParamSpec("tileSize", "tileSize", "Tile size")
	};

	public static class ArraySizePatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix new array[" + this.getFromSize() + "] -> " + this.getToSize(); }

		public byte[] getBytes(int size) {
			return new byte[]{
				SIPUSH, b(size, 1), b(size, 0),
				(byte) NEWARRAY
			};
		}
	}

	public static class WhilePatch extends BytecodeTilePatch {
		public String getDescription() { return String.format("Fix while(i<%d) -> while(i<%d)", this.getFromSize(), this.getToSize()); }

		public byte[] getBytes(int size) {
			if(size < 0xFF) {
				return new byte[]{
					BIPUSH, b(size, 0),
					(byte) IF_ICMPGE
				};
			} else {
				return new byte[]{
					SIPUSH, b(size, 1), b(size, 0),
					(byte) IF_ICMPGE
				};
			}
		}
	}

	public static class BitMaskPatch extends BytecodeTilePatch {
		public String getDescription() { return String.format("Fix &%x -> %x", this.getFromSize(), this.getToSize()); }

		public byte[] getBytes(int cnt) {
			if(cnt>0) cnt = cnt - 1;
			if(cnt < 0xFF) {
				return new byte[]{
					BIPUSH, b(cnt, 0),
					(byte) IAND
				};
			} else {
				return new byte[]{
					SIPUSH, b(cnt, 1), b(cnt, 0),
					(byte) IAND
				};
			}
		}
	}

	public static class MultiplyPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix * " + this.getFromSize() + " -> " + this.getToSize(); }

		public byte[] getBytes(int cnt) {
			if(cnt < 0xFF) {
				return new byte[]{
					BIPUSH, b(cnt, 0),
					(byte) IMUL
				};
			} else {
				return new byte[]{
					SIPUSH, b(cnt, 1), b(cnt, 0),
					(byte) IMUL
				};
			}
		}
	}

	public static class ModPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix mod " + this.getFromSize() + " -> " + this.getToSize(); }

		public byte[] getBytes(int cnt) {
			if(cnt < 0xFF) {
				return new byte[]{
					BIPUSH, b(cnt, 0),
					(byte) IREM
				};
			} else {
				return new byte[]{
					SIPUSH, b(cnt, 1), b(cnt, 0),
					(byte) IREM
				};
			}
		}
	}

	public static class ModMulPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix %16*"+getFromSize()+"+_3*"+getFromSize()+" -> %16*"+getToSize()+"+_3*"+getToSize(); }

		public byte[] getBytes(int size) {
			return new byte[]{
				BIPUSH, 16,
				IREM,
				BIPUSH, b(size, 0),
				IMUL,
				ILOAD_3,
				BIPUSH, b(size, 0),
				IMUL
			};
		}
	}

	public static class DivMulPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix /16*"+getFromSize()+"+_4*"+getFromSize()+" -> /16*"+getToSize()+"+_4*"+getToSize(); }

		public byte[] getBytes(int size) {
			return new byte[]{
				BIPUSH, 16,
				IDIV,
				BIPUSH, b(size, 0),
				IMUL,
				ILOAD, 4,
				BIPUSH, b(size, 0),
				IMUL
			};
		}
	}

	public static class SubImagePatch extends BytecodeTilePatch {
		public String getDescription() {
			return String.format("glTexSubImage2D(...,%1$d,%1$d) -> glTexSubImage2D(...,%2$d,%2$d)",
				this.getFromSize(), this.getToSize());
		}

		public byte[] getBytes(int size) {
			return new byte[]{
				BIPUSH, b(size, 0),
				BIPUSH, b(size, 0),
				SIPUSH, 0x19, 0x08,
				SIPUSH, 0x14, 0x01,
				/* would be nice to make this more specific, but we'd have to look up the call */
			};
		}
	}

	public static class VarCmpPatch extends BytecodeTilePatch {
		public String getDescription() {
			return String.format("Fix ILOAD_%1$d; %2$s %3$d -> %2$s %4$d",
				vnum, Mnemonic.OPCODE[comparison], this.getFromSize(), this.getToSize());
		}
		int vnum, comparison;
		public VarCmpPatch(int vnum, int comparison) {
			this.vnum = vnum;
			this.comparison = comparison;
		}
		public byte[] getBytes(int size) {
			if(vnum < 4) {
				return new byte[]{
					(byte)(ILOAD_0 + vnum),
					BIPUSH, (byte)size,
					(byte)this.comparison
				};
			} else {
				return new byte[]{
					ILOAD, (byte)vnum,
					BIPUSH, (byte)size,
					(byte)this.comparison
				};
			}
		}

	}

	public static class FireUnpatch extends BytecodeTilePatch {
		public String getDescription() { return "(unpatch) <init> *"+this.getToSize()+" to *"+this.getFromSize(); }
		public byte[] getFromBytes() throws Exception { return super.getToBytes(); }
		public byte[] getToBytes() throws Exception { return super.getFromBytes(); }
		public byte[] getBytes(int size) {
			return new byte[]{
				ILOAD_1,
				BIPUSH, (byte)size,
				IMUL
			};
		}
	}

	public static class CompassGetRGBPatch extends BytecodeTilePatch {
		public String getDescription() {
			return String.format(".getRGB(...%1$d,%1$d,...%1$d) to .getRGB(...%2$d,%2$d,...%2$d)",
				this.getFromSize(), this.getToSize());
		}
		public byte[] getBytes(int size) {
			return new byte[]{
				BIPUSH, (byte)size,
				BIPUSH, (byte)size,
				ALOAD_0,
				(byte)GETFIELD, 0x00, 0x2B,
				ICONST_0,
				BIPUSH, (byte)size,
			};
		}
	}

	public static class OverrideTilenumPatch extends BytecodePatch {
		public String getDescription() {return "Change tile number";}
		public ParamSpec[] getParamSpecs() { return PSPEC_EMPTY; }

		int from, to;
		public OverrideTilenumPatch(int from, int to) {
			this.from = from;
			this.to = to;
		}

		byte[] getFromBytes() {
			return new byte[]{
				(byte)ALOAD_0,
				(byte)ILOAD_1,
				(byte)PUTFIELD, 0x00, 0x08
			};
		}

		byte[] getToBytes() {
			return new byte[]{
				(byte)ALOAD_0,
				(byte)ILOAD_1,
				(byte)PUTFIELD, 0x00, 0x08, // keep so extra patches work
				(byte)ILOAD_1,
				(byte)SIPUSH, 0x00, (byte)this.from,
				(byte)IF_ICMPNE, 0, 10, // bytes to jump forward
				(byte)ALOAD_0,
				(byte)SIPUSH, 0x00, (byte)160,
				(byte)PUTFIELD, 0x00, 0x08,
			};
		}

	}

	public static final PatchSet water = new PatchSet(
		"Water",
		new PatchSpec[]{
			new PatchSpec(new ArraySizePatch().square(true)),
			new PatchSpec(new WhilePatch()),
			new PatchSpec(new WhilePatch().square(true)),
			new PatchSpec(new BitMaskPatch()),
			new PatchSpec(new BitMaskPatch().square(true)),
			new PatchSpec(new MultiplyPatch())
		}
	);

	public static final PatchSet animManager = new PatchSet(
		"AnimManager",
		new PatchSpec[]{
			new PatchSpec(new ModMulPatch()),
			new PatchSpec(new DivMulPatch()),
			new PatchSpec(new SubImagePatch())
		}
	);

	public static final PatchSet animTexture = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new ArraySizePatch().square(true).multiplier(4))
		}
	);

	public static final PatchSet fire = new PatchSet(
		"Fire",
		new PatchSpec[]{
			new PatchSpec(new ArraySizePatch().square(true).addY(4)),
			new PatchSpec(new WhilePatch()),
			new PatchSpec(new WhilePatch().square(true)),
			new PatchSpec(new WhilePatch().add(4)),
			new PatchSpec(new MultiplyPatch()),
			new PatchSpec(new ModPatch().add(4)),
			new PatchSpec(new VarCmpPatch(2, IF_ICMPLT).add(3)),
			new PatchSpec(new FireUnpatch()),
			new PatchSpec(new ConstPatch(1.06F, 1.03F))
		}
	);

	public static final PatchSet compass = new PatchSet(
		"Compass",
		new PatchSpec[]{
			new PatchSpec(new ArraySizePatch().square(true)),
			new PatchSpec(new ArraySizePatch().square(true).addY(4)),
			new PatchSpec(new WhilePatch().square(true)),
			new PatchSpec(new MultiplyPatch()),
			new PatchSpec(new CompassGetRGBPatch()),
			new PatchSpec(new ConstPatch(8.5D, 16.5D)),
			new PatchSpec(new ConstPatch(7.5D, 15.5D)),
		}
	);

	public static final PatchSet hideWater = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new OverrideTilenumPatch(12*16+13+0, 160)),
			new PatchSpec(new OverrideTilenumPatch(12*16+13+1, 160)),
		}
	);

	public static final PatchSet hideLava = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new OverrideTilenumPatch(14*16+13+0, 160)),
			new PatchSpec(new OverrideTilenumPatch(14*16+13+1, 160)),
		}
	);

	public static final PatchSet hideFire = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new OverrideTilenumPatch(31+(0*16), 160)),
			new PatchSpec(new OverrideTilenumPatch(31+(1*16), 160)),
		}
	);

}