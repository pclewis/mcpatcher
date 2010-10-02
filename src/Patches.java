import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;

class Patches implements Opcode {
	public static ParamSpec[] PSPEC_EMPTY = new ParamSpec[]{};
	public static ParamSpec[] PSPEC_TILESIZE = new ParamSpec[]{
		new ParamSpec("tileSize", "tileSize", "Tile size")
	};

	class ArraySizePatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix array allocations"; }

		public byte[] getBytes(int size) {
			return new byte[]{
				SIPUSH, b(size, 0), b(size, 1),
				(byte) NEWARRAY
			};
		}
	}

	class WhilePatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix while loops"; }

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

	class BitMaskPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix bit masking operations"; }

		public byte[] getBytes(int cnt) {
			cnt = cnt - 1;
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

	class MultiplyPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix multiplication operations"; }

		public byte[] getBytes(int cnt) {
			cnt = cnt - 1;
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

	class ModPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix modulus operations"; }

		public byte[] getBytes(int cnt) {
			cnt = cnt - 1;
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

	class ModMulPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix %16*16+_3*16 -> %16*x+_3*x"; }

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

	class DivMulPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix %16*16+_4*16 -> %16*x+_4*x"; }

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

	class SubImagePatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix calls to glTexSubImage2D(...,16,16)"; }

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

	class VarCmpPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix var comparisons"; }
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

	class FireUnpatch extends BytecodeTilePatch {
		public String getDescription() { return "(unpatch) <init> *32 to *16"; }
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


	final PatchSet waterPatches = new PatchSet(
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

	final PatchSet animManager = new PatchSet(
		"AnimManager",
		new PatchSpec[]{
			new PatchSpec(new ModMulPatch()),
			new PatchSpec(new DivMulPatch()),
			new PatchSpec(new SubImagePatch())
		}
	);

	final PatchSet animTexture = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new ArraySizePatch().square(true).multiplier(4))
		}
	);

	final PatchSet fireTexture = new PatchSet(
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
			new PatchSpec(new ConstPatch<Float>(ConstPool.CONST_Float, 1.04F, 1.03F))
		}
	);
}