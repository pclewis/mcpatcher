public class PatchSpec {
	public Patch patch;
    public boolean required;
    public boolean enabled;

    public PatchSpec(Patch patch) {
        this(patch, true);
    }

    public PatchSpec(Patch patch, boolean required) {
        this.patch = patch;
        this.required = required;
        this.enabled = true;
    }

	public PatchSpec(PatchSpec src) {
		try {
			this.patch = src.patch.clone();
		} catch(CloneNotSupportedException e) {
			throw new RuntimeException("Clone not supported on Patch");
		}
		this.required = src.required;
		this.enabled = src.enabled;
	}

	public Patch getPatch() {
		return patch;
	}

	public boolean isRequired() {
		return required;
	}

	public boolean isEnabled() {
		return enabled;
	}
}
