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
}
