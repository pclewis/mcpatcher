import javassist.bytecode.*;

class Patches implements Opcode {
	public static ParamSpec[] PSPEC_EMPTY = new ParamSpec[]{};
	public static ParamSpec[] PSPEC_TILESIZE = new ParamSpec[]{
		new ParamSpec("tileSize", "tileSize", "Tile size")
	};

	public static class ArraySizePatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix new array[" + this.getFromSize() + "] -> " + this.getToSize(); }

		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				push(mi, size),
				NEWARRAY
			);
		}
	}

	public static class WhilePatch extends BytecodeTilePatch {
		public String getDescription() { return String.format("Fix while(i<%d) -> while(i<%d)", this.getFromSize(), this.getToSize()); }

		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				push(mi, size),
				IF_ICMPGE
			);
		}
	}

	public static class BitMaskPatch extends BytecodeTilePatch {
		public String getDescription() { return String.format("Fix &0x%x -> 0x%x", this.getFromSize(), this.getToSize()); }

		public byte[] getBytes(int size, MethodInfo mi) {
			if(size>0) size = size - 1;
			return buildCode(
				push(mi, size),
				IAND
			);
		}
	}

	public static class MultiplyPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix * " + this.getFromSize() + " -> " + this.getToSize(); }

		public byte[] getBytes(int cnt, MethodInfo mi) {
			return buildCode( push(mi, cnt), IMUL );
		}
	}

	public static class ModPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix mod " + this.getFromSize() + " -> " + this.getToSize(); }

		public byte[] getBytes(int cnt, MethodInfo mi) {
			return buildCode( push(mi, cnt), IREM );
		}
	}

	public static class ModMulPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix %16*"+getFromSize()+" -> %16*"+getToSize(); }

		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				BIPUSH, 16,
				IREM,
				push(mi, size),
				IMUL
			);
		}
	}
	public static class ModMulMulPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix %16*"+getFromSize()+"+_3*"+getFromSize()+" -> %16*"+getToSize()+"+_3*"+getToSize(); }

		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				BIPUSH, 16,
				IREM,
				push(mi, size),
				IMUL,
				ILOAD_3,
				push(mi, size),
				IMUL
			);
		}
	}

	public static class DivMulPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix /16*"+getFromSize()+" -> /16*"+getToSize(); }

		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				BIPUSH, 16,
				IDIV,
				push(mi, size),
				IMUL
			);
		}
	}
	public static class DivMulMulPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix /16*"+getFromSize()+"+_4*"+getFromSize()+" -> /16*"+getToSize() + "+_4*"+getToSize(); }

		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				BIPUSH, 16,
				IDIV,
				push(mi, size),
				IMUL,
				ILOAD, 4,
				push(mi, size),
				IMUL
			);
		}
	}

	public static class SubImagePatch extends BytecodeTilePatch {
		public String getDescription() {
			return String.format("glTexSubImage2D(...,%1$d,%1$d) -> glTexSubImage2D(...,%2$d,%2$d)",
				this.getFromSize(), this.getToSize());
		}

		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				push(mi, size),
				push(mi, size),
				SIPUSH, 0x19, 0x08,
				SIPUSH, 0x14, 0x01
			);	/* would be nice to make this more specific, but we'd have to look up the call */
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
		public byte[] getBytes(int size, MethodInfo mi) {
			Object iload = vnum < 4 ? (ILOAD_0 + vnum) : new byte[]{ ILOAD, (byte)vnum };
			return buildCode(
				iload,
				push(mi, size),
				this.comparison
			);
		}

	}

	public static class FireUnpatch extends BytecodeTilePatch {
		public String getDescription() { return "(unpatch) <init> *"+this.getToSize()+" to *"+this.getFromSize(); }
		public byte[] getFromBytes(MethodInfo mi) throws Exception { return super.getToBytes(mi); }
		public byte[] getToBytes(MethodInfo mi) throws Exception { return super.getFromBytes(mi); }
		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				ILOAD_1,
				push(mi, size),
				IMUL
			);
		}
	}

	public static class CompassGetRGBPatch extends BytecodeTilePatch {
		public String getDescription() {
			return String.format(".getRGB(...%1$d,%1$d,...%1$d) to .getRGB(...%2$d,%2$d,...%2$d)",
				this.getFromSize(), this.getToSize());
		}
		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				push(mi, size),
				push(mi, size),
				ALOAD_0,
				GETFIELD, 0x00, 0x2B,
				ICONST_0,
				push(mi, size)
			);
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

		byte[] getFromBytes(MethodInfo mi) {
			return new byte[]{
				(byte)ALOAD_0,
				(byte)ILOAD_1,
				(byte)PUTFIELD, 0x00, 0x08
			};
		}

		byte[] getToBytes(MethodInfo mi) {
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

	public static class ConstSquarePatch extends ConstTilePatch {
		protected Object getValue(int tileSize) {
			return (float)(tileSize*tileSize);
		}
	}

	public static class ConstMemPatch extends ConstTilePatch {
		protected Object getFrom() {
			return (Integer) 1048576;
		}
		protected Object getValue(int tileSize) {
			return (Integer) ((tileSize*16)*(tileSize*16)*4);
		}
	}

	public static class ConstFirePatch extends ConstTilePatch {
		protected Object getValue(int tileSize) {
			float fireSize = 1.06F;
			if(tileSize==16) fireSize = 1.06F;
			else if(tileSize==32) fireSize = 1.03F;
			else if(tileSize==64) fireSize = 1.02F;
			else if(tileSize==128) fireSize = 1.01F;
			else if(tileSize==256) fireSize = 1.005F;
			return (Float) fireSize;
		}
	}

	public static class ConstCompassPatch extends ConstTilePatch {
		int dir = 0;
		public ConstCompassPatch(int dir) { this.dir = dir; }
		protected Object getValue(int tileSize) {
			return (Double) (tileSize/2 + (0.5D * dir));

		}
	}

	public static class ToolTopPatch extends BytecodePatch {
		public ParamSpec[] getParamSpecs() { return PSPEC_EMPTY; }
		public String getDescription() {
			return String.format("tool pixel top");
		}
		public byte[] getFromBytes(MethodInfo mi) {
			return new byte[] {
				(byte)LDC, (byte)0x0D,
				(byte)FADD,
				(byte)FSTORE, 0x0F,
				(byte)ALOAD_2,
				(byte)DCONST_0
			};
		}
		public byte[] getToBytes(MethodInfo mi) {
			return new byte[] {
				(byte)NOP, (byte)NOP,
				(byte)NOP,
				(byte)FSTORE, 0x0F,
				(byte)ALOAD_2,
				(byte)DCONST_0
			};
		}
	}

	public static class ToolTexPatch extends BytecodeTilePatch {
		public String getDescription() { return "Fix tool tex nonsense"; }
		int op;
		boolean add;
		public ToolTexPatch(int op, boolean add) { this.op = op; this.add=add; }

		public byte[] getBytes(int size, MethodInfo mi) {
			return buildCode(
				BIPUSH, 16,
				op,
				push(mi, size),
				IMUL,
				push(mi, add ? size : 0),
				IADD,
				I2F,
				ConstPoolUtils.getLoad(mi.getConstPool(), (float)(16*size))
			);
		}
	}

	public static class ConstTileSizePatch extends ConstTilePatch {
		public Object getValue(int tileSize) {
			return (float)tileSize;
		}
	}

	public static class TexNudgePatch extends ConstTilePatch {
		public Object getValue(int tileSize) {
			return (float) 1F/(tileSize*tileSize*2);
		}
	}

	public static class PassThisPatch extends BytecodePatch {
		public ParamSpec[] getParamSpecs() { return PSPEC_EMPTY; }
		public String getDescription() { return "pass Minecraft to "+className+"."+methodName; }

		String className, methodName, fromType, toType;

		public PassThisPatch(String className, String methodName, String fromType, String toType) {
			this.className = className;
			this.methodName = methodName;
			this.fromType = fromType;
			this.toType = toType;
		}

		byte[] getFromBytes(MethodInfo mi) throws Exception {
			int methi = ConstPoolUtils.find(mi.getConstPool(), new MethodRef(className, methodName, fromType));
			return buildCode(
				(byte)INVOKESPECIAL, Util.b(methi, 1), Util.b(methi, 0)
			);
		}

		byte[] getToBytes(MethodInfo mi) throws Exception {
			int methi = ConstPoolUtils.findOrAdd(mi.getConstPool(), new MethodRef(className, methodName, toType));
			return buildCode(
				(byte)ALOAD_0,
				(byte)INVOKESPECIAL, Util.b(methi, 1), Util.b(methi, 0)
			);
		}
	}

	public static final PatchSet water = new PatchSet(
		"Water",
		new PatchSpec[]{
			new PatchSpec(new ArraySizePatch().square(true)),
			/* Order is important! if newSize=origSize.square()... */
			new PatchSpec(new WhilePatch().square(true)),
			new PatchSpec(new WhilePatch()),
			new PatchSpec(new BitMaskPatch().square(true)),
			new PatchSpec(new BitMaskPatch()),
			new PatchSpec(new MultiplyPatch()),
			new PatchSpec(new ConstSquarePatch()),
		}
	);

	public static final PatchSet animManager = new PatchSet(
		"AnimManager",
		new PatchSpec[]{
			new PatchSpec(new ModMulMulPatch()),
			new PatchSpec(new DivMulMulPatch()),
			new PatchSpec(new SubImagePatch()),
			new PatchSpec(new ConstMemPatch())
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
			new PatchSpec(new WhilePatch().square(true)),
			new PatchSpec(new WhilePatch().add(4)),
			new PatchSpec(new WhilePatch()),
			new PatchSpec(new MultiplyPatch()),
			new PatchSpec(new ModPatch().add(4)),
			new PatchSpec(new VarCmpPatch(2, IF_ICMPLT).add(3)),
			new PatchSpec(new FireUnpatch()),
			new PatchSpec(new ConstFirePatch())
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
			new PatchSpec(new ConstCompassPatch(1)),
			new PatchSpec(new ConstCompassPatch(-1)),
		}
	);

	public static final PatchSet hideWater = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new OverrideTilenumPatch(12*16+13+0, 160)),
			new PatchSpec(new OverrideTilenumPatch(12*16+13+1, 160)),
		}
	);
	public static final PatchSet hideStillWater = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new OverrideTilenumPatch(12*16+13+0, 160)),
		}
	);

	public static final PatchSet hideLava = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new OverrideTilenumPatch(14*16+13+0, 160)),
			new PatchSpec(new OverrideTilenumPatch(14*16+13+1, 160)),
		}
	);

	public static final PatchSet hideStillLava = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new OverrideTilenumPatch(14*16+13+0, 160)),
		}
	);

	public static final PatchSet hideFire = new PatchSet(
		"AnimTexture",
		new PatchSpec[]{
			new PatchSpec(new OverrideTilenumPatch(31+(0*16), 160)),
			new PatchSpec(new OverrideTilenumPatch(31+(1*16), 160)),
		}
	);

	public static final PatchSet tool3d = new PatchSet(
		"Tool3D",
		new PatchSpec[]{
			new PatchSpec(new ToolTopPatch()),
			new PatchSpec(new ConstTileSizePatch()),
			new PatchSpec(new ToolTexPatch(IREM, false)),
			new PatchSpec(new ToolTexPatch(IDIV, false)),
			new PatchSpec(new ToolTexPatch(IREM, true)),
			new PatchSpec(new ToolTexPatch(IDIV, true)),
			new PatchSpec(new WhilePatch()),
			new PatchSpec(new TexNudgePatch()),
		}
	);

	public static final PatchSet customWaterMC = new PatchSet(
		"Minecraft",
		new PatchSpec[] {
			new PatchSpec(new PassThisPatch("ht", "<init>", "()V", "(Lnet/minecraft/client/Minecraft;)V")),
			new PatchSpec(new PassThisPatch("ml", "<init>", "()V", "(Lnet/minecraft/client/Minecraft;)V")),
		}
	);

	public static final PatchSet customLavaMC = new PatchSet(
		"Minecraft",
		new PatchSpec[] {
			new PatchSpec(new PassThisPatch("eg", "<init>", "()V", "(Lnet/minecraft/client/Minecraft;)V")),
			new PatchSpec(new PassThisPatch("at", "<init>", "()V", "(Lnet/minecraft/client/Minecraft;)V")),
		}
	);

	public static final PatchSet betterGrass = new PatchSet(
		"Block",
		new PatchSpec[] {
			new PatchSpec(new ConstPatch(new ClassRef("my"), new ClassRef("BetterGrass"))),
			new PatchSpec(new ConstPatch(new MethodRef("my", "<init>", "(I)V"),
										 new MethodRef("BetterGrass","<init>", "(I)V")))
		}
	);

	public static final PatchSet electricCart = new PatchSet(
		"jo",
		new PatchSpec[] {
			new PatchSpec(new ConstPatch(new ClassRef("oc"), new ClassRef("ElectricCart"))),
			new PatchSpec(new ConstPatch(new MethodRef("oc", "<init>", "(Lcn;DDDI)V"),
										 new MethodRef("ElectricCart","<init>", "(Lcn;DDDI)V")))
		}
	);

	public static final PatchSet cart = new PatchSet(
		"oc",
		new PatchSpec[] {
			new PatchSpec(new ConstPatch(0.4D, 1.0D))
		}
	);

	public static final PatchSet electricWire = new PatchSet(
		"ly",
		new PatchSpec[] {
			new PatchSpec(new ConstPatch(new ClassRef("kf"), new ClassRef("ElectricWire"))),
			new PatchSpec(new ConstPatch(new MethodRef("kf", "<init>", "(II)V"),
										 new MethodRef("ElectricWire", "<init>", "(II)V"))),
		}
	);

	public static final PatchSet wire = new PatchSet(
		"Wire",
		new PatchSpec[] {
			new PatchSpec(new Patch() {
				public ParamSpec[] getParamSpecs() { return PSPEC_EMPTY; }
				public String getDescription() { return "make .h protected"; }
				public void visitConstPool(ConstPool cp) throws Exception { }
				public void visitMethod(MethodInfo mi) throws Exception {
					if(mi.getName().equals("h") || mi.getName().equals("i")) {
						MCPatcher.out.println("make " + mi.getName() + " protected");
						mi.setAccessFlags( mi.getAccessFlags() & ~AccessFlag.PRIVATE | AccessFlag.PROTECTED );
					}
				}
			}),
			new PatchSpec(new BytecodePatch() {
				public ParamSpec[] getParamSpecs() { return PSPEC_EMPTY; }
				public String getDescription() { return "invoke .h virtual"; }

				byte[] getFromBytes(MethodInfo mi) throws Exception {
					int i = ConstPoolUtils.find(mi.getConstPool(), new MethodRef("kf", "h", "(Lcn;III)V"));
					return buildCode(INVOKESPECIAL, Util.b(i, 1), Util.b(i, 0));
				}

				byte[] getToBytes(MethodInfo mi) throws Exception {
					int i = ConstPoolUtils.find(mi.getConstPool(), new MethodRef("kf", "h", "(Lcn;III)V"));
					return buildCode(INVOKEVIRTUAL, Util.b(i, 1), Util.b(i, 0));
				}
			})
		}
	);
}