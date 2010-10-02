class Patches {
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
}