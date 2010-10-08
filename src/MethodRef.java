public class MethodRef {
	String className;

	public String getClassName() {
		return className;
	}

	String name;
	String type;

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public MethodRef(String className, String name, String type) {
		this.className = className;
		this.name = name;
		this.type = type;
	}

	public String toString() {
		return "MethodRef{" +
			"className='" + className + '\'' +
			", name='" + name + '\'' +
			", type='" + type + '\'' +
			'}';
	}
}
