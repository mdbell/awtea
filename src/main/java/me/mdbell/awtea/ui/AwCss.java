package me.mdbell.awtea.ui;

import org.teavm.jso.dom.html.HTMLElement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class AwCss {

	private AwCss() {
	}

	public static Set<Number> injects = new HashSet<>();

	public static Sheet sheet() {
		return new Sheet();
	}

	public static String var(String cssVarName) {
		// e.g. "--aw-error-fg" -> "var(--aw-error-fg)"
		return "var(" + cssVarName + ")";
	}

	// ================== internal types ==================

	public interface CssValue {
		String toCssValue();
	}

	public interface CssKey extends CssValue {
		@Override
		String toCssValue();

		String toCssKey();
	}

	public static final class Sheet implements CssValue {
		private final StringBuilder sb = new StringBuilder();

		private Sheet() {

		}

		public void inject() {
			int hash = this.toCssValue().hashCode();
			if (!injects.add(hash)) {
				return;
			}

			HTMLElement style = org.teavm.jso.browser.Window.current()
				.getDocument()
				.createElement("style");
			style.setTextContent(this.toCssValue());
			org.teavm.jso.browser.Window.current()
				.getDocument()
				.getHead()
				.appendChild(style);
		}

		public Rule<?> rule(String selector) {
			sb.append(selector).append(" {");
			return new Rule<>(this);
		}

		public CssClass createClass(String name) {
			sb.append('.').append(name).append(" {");
			return new CssClass(this, name);
		}

		public Sheet raw(String css) {
			sb.append(css);
			return this;
		}

		StringBuilder builder() {
			return sb;
		}

		@Override
		public String toCssValue() {
			return sb.toString();
		}
	}

	public static final class CssClass extends Rule<CssClass> implements CssValue {
		private final String name;

		public CssClass(Sheet sheet, String name) {
			super(sheet);
			this.name = name;
		}

		public CssClass before() {
			return end().
				createClass(name + "::before");

		}

		public CssClass after() {
			return end().
				createClass(name + "::after");
		}

		public CssClass subClass(String subName) {
			return end().
				createClass(name + " ." + subName);
		}

		public CssClass createClass(String name) {
			return end().
				createClass(name);
		}

		@Override
		public String toCssValue() {
			return name;
		}
	}

	public static class Property<T extends Rule<T>> {
		private final T rule;
		private final Sheet sheet;

		Property(T rule) {
			this.rule = rule;
			this.sheet = rule.sheet;
		}

		public Property<T> value(String... value) {
			String joinedValue = String.join(" ", value);
			sheet.builder()
				.append(' ')
				.append(joinedValue);
			return this;
		}

		public Property<T> value(CssValue... values) {
			String[] stringValues = Arrays.stream(values)
				.map(CssValue::toCssValue)
				.toArray(String[]::new);
			return value(stringValues);
		}

		public T end() {
			sheet.builder().append(';');
			return rule;
		}
	}

	public static class Rule<T extends Rule<T>> {
		final Sheet sheet;

		Rule(Sheet sheet) {
			this.sheet = sheet;
		}

		public Property<T> prop(String name) {
			sheet.builder().append(name).append(':');
			return new Property((T) this);
		}

		public T prop(String name, String... value) {
			String joinedValue = String.join(" ", value);
			sheet.builder()
				.append(name).append(':')
				.append(joinedValue)
				.append(';');
			return (T) this;
		}

		public T prop(CssKey key, CssValue value) {
			return prop(key.toCssKey(), value);
		}

		public T prop(CssKey key, String value) {
			return prop(key.toCssKey(), value);
		}

		public T prop(String name, CssValue value) {
			return prop(name, value.toCssValue());
		}

		public Sheet end() {
			sheet.builder().append('}');
			return sheet;
		}
	}
}
