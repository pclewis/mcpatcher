public class PatchSet {
    private String className;
    private PatchSpec[] patchSet;

    public PatchSet(String className, PatchSpec[] patchSet) {
        this.className = className;
        this.patchSet = patchSet;
    }
}
