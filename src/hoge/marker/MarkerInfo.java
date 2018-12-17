package hoge.marker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.Signature;

/**
 * Expression of methods refereed from marker.
 */
public class MarkerInfo {
	String fqcn, methodName;
	String[] args;
	static Pattern markerPattern = Pattern.compile("^\\d[:](.+)#([a-zA-Z0-9_$]+)#(.*)$");

	public MarkerInfo(String fqcn, String methodName, String[] args) {
		this.fqcn = fqcn;
		this.methodName = methodName;
		this.args = args;
	}

	/**
	 * @return className
	 */
	public String getClassName() {
		return fqcn;
	}

	/**
	 * Return JVM format signature(lack return type). as: public_add (II)
	 * 
	 * @return JVM signature format(lack return type)
	 */
	public String toSignatureString() {
		StringBuilder builder = new StringBuilder();
		builder.append(methodName);
		builder.append(" (");
		builder.append(String.join("", args));
		builder.append(")");

		return new String(builder);
	}

	/**
	 * Convert raw marker attribute string to MarkerInfo.
	 * 
	 * @param raw
	 * @return converted instance
	 */
	public static MarkerInfo convertFromRaw(String raw) {
		Matcher mark = MarkerInfo.markerPattern.matcher(raw);
		mark.matches();

		String[] args = Stream.of(mark.group(3).replaceAll(" ", "").split("[,#]")).filter(str -> !str.isEmpty())
				.map(arg -> Signature.createTypeSignature(arg, true).replace('.', '/')).collect(Collectors.toList())
				.toArray(new String[0]);
		MarkerInfo markerInfo = new MarkerInfo(mark.group(1), mark.group(2), args);

		return markerInfo;
	}
}