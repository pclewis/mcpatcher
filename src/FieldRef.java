public class FieldRef {
	String className;
	String name;
	String type;

	public FieldRef(String className, String name, String type) {
		this.className = className;
		this.name = name;
		this.type = type;
	}

	public String getClassName() {
		return className;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String toString() {
		return "FieldRef{" +
		       "className='" + className + '\'' +
		       ", name='" + name + '\'' +
		       ", type='" + type + '\'' +
		       '}';
	}
}