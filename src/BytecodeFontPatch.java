public abstract class BytecodeFontPatch extends BytecodePatch {
	public ParamSpec[] getParamSpecs() { return Patches.PSPEC_EMPTY; }
	abstract public String getDescription();

	public static final byte IMAGE_WIDTH_REGISTER = 5;  // TODO: determine this automatically
	public static final byte IMAGE_HEIGHT_REGISTER = IMAGE_WIDTH_REGISTER + 1;
	public byte getRegister() {
		return IMAGE_WIDTH_REGISTER;
	}
}
