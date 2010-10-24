package com.pclewis.mcpatcher;

public abstract class ConstTilePatch extends ConstPatch {
	private int tag = -1;

	abstract protected Object getValue(int tileSize);

	protected Object getFrom() { return getValue(16); }
	protected Object getTo() { return getValue(params.getInt("tileSize")); }
	protected int getTag() {
		if(tag==-1) tag = ConstPoolUtils.getTag(getFrom());
		return tag;
	}

    public ParamSpec[] getParamSpecs() {
        return Patches.PSPEC_TILESIZE;
    }
}
