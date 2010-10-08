public class ClassRef {
	public String getClassName() {
		return className;
	}

	private String className;

	public ClassRef(String className) {
		this.className = className;
	}

	public String toString() {
		return "ClassRef{" +
			"className='" + className + '\'' +
			'}';
	}
}
