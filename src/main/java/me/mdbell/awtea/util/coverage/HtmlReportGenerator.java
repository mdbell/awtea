package me.mdbell.awtea.util.coverage;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Generates HTML coverage reports
 */
public class HtmlReportGenerator {

	public void generate(CoverageData data, Path outputPath) throws IOException {
		if (outputPath.getParent() != null) {
			Files.createDirectories(outputPath.getParent());
		}
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
			writeHtml(data, writer);
		}
	}

	private void writeHtml(CoverageData data, PrintWriter out) {
		out.println("<!DOCTYPE html>");
		out.println("<html lang=\"en\">");
		out.println("<head>");
		out.println("    <meta charset=\"UTF-8\">");
		out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
		out.println("    <title>AWT API Coverage Report</title>");
		out.println("    <style>");
		writeStyles(out);
		out.println("    </style>");
		out.println("</head>");
		out.println("<body>");
		out.println("    <div class=\"container\">");
		out.println("        <h1>AWT API Coverage Report</h1>");
		
		if (data.getTimestamp() != null) {
			out.printf("        <p class=\"timestamp\">Generated: %s</p>%n", escapeHtml(data.getTimestamp()));
		}
		
		writeSummary(data, out);
		writePackages(data, out);
		
		out.println("    </div>");
		writeScript(out);
		out.println("</body>");
		out.println("</html>");
	}

	private void writeStyles(PrintWriter out) {
		out.println("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }");
		out.println("        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
		out.println("        h1 { color: #333; margin-top: 0; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }");
		out.println("        h2 { color: #555; margin-top: 30px; cursor: pointer; user-select: none; }");
		out.println("        h2:hover { color: #4CAF50; }");
		out.println("        h3 { color: #666; margin-top: 20px; margin-left: 20px; cursor: pointer; user-select: none; }");
		out.println("        h3:hover { color: #4CAF50; }");
		out.println("        .timestamp { color: #888; font-size: 0.9em; margin-top: -10px; }");
		out.println("        .summary { background: #f9f9f9; padding: 20px; border-radius: 5px; margin: 20px 0; }");
		out.println("        .summary-item { margin: 10px 0; }");
		out.println("        .progress-bar { width: 100%; height: 30px; background: #e0e0e0; border-radius: 15px; overflow: hidden; margin: 10px 0; }");
		out.println("        .progress-fill { height: 100%; background: linear-gradient(90deg, #4CAF50, #8BC34A); display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; transition: width 0.3s; }");
		out.println("        .progress-fill.low { background: linear-gradient(90deg, #f44336, #e57373); }");
		out.println("        .progress-fill.medium { background: linear-gradient(90deg, #FF9800, #FFB74D); }");
		out.println("        .package { margin: 20px 0; border: 1px solid #ddd; border-radius: 5px; }");
		out.println("        .package-header { background: #f0f0f0; padding: 15px; border-radius: 5px 5px 0 0; }");
		out.println("        .package-content { padding: 15px; display: none; }");
		out.println("        .package-content.expanded { display: block; }");
		out.println("        .class { margin: 15px 0; border-left: 3px solid #4CAF50; padding-left: 15px; }");
		out.println("        .class.partial { border-left-color: #FF9800; }");
		out.println("        .class.missing { border-left-color: #f44336; }");
		out.println("        .class-header { background: #fafafa; padding: 10px; border-radius: 3px; }");
		out.println("        .class-content { padding: 10px; margin-left: 20px; display: none; }");
		out.println("        .class-content.expanded { display: block; }");
		out.println("        .section { margin: 15px 0; }");
		out.println("        .section-title { font-weight: bold; color: #555; margin-bottom: 5px; }");
		out.println("        .section-title.collapsible { cursor: pointer; user-select: none; padding: 8px; border-radius: 3px; background: #f5f5f5; }");
		out.println("        .section-title.collapsible:hover { background: #e0e0e0; }");
		out.println("        .item-list { list-style: none; padding: 0; margin: 10px 0; }");
		out.println("        .item-list li { padding: 5px 10px; margin: 3px 0; background: #f9f9f9; border-radius: 3px; font-family: 'Courier New', monospace; font-size: 0.9em; }");
		out.println("        .item-list.implemented li { background: #e8f5e9; border-left: 3px solid #4CAF50; }");
		out.println("        .item-list.missing li { background: #ffebee; border-left: 3px solid #f44336; }");
		out.println("        .coverage-badge { display: inline-block; padding: 5px 10px; border-radius: 3px; font-size: 0.9em; font-weight: bold; }");
		out.println("        .coverage-badge.full { background: #4CAF50; color: white; }");
		out.println("        .coverage-badge.partial { background: #FF9800; color: white; }");
		out.println("        .coverage-badge.low { background: #f44336; color: white; }");
		out.println("        .toggle-icon { font-size: 0.8em; margin-right: 5px; }");
	}

	private void writeSummary(CoverageData data, PrintWriter out) {
		out.println("        <div class=\"summary\">");
		out.println("            <h2>Summary</h2>");
		out.printf("            <div class=\"summary-item\"><strong>Total Coverage:</strong> %d / %d (%s)</div>%n",
			data.getTotalImplemented(), data.getTotalRuntime(), formatPercentage(data.getTotalPercentage()));
		writeProgressBar(out, data.getTotalPercentage());
		out.printf("            <div class=\"summary-item\"><strong>Packages:</strong> %d</div>%n", data.getPackages().size());
		
		int totalClasses = data.getPackages().values().stream()
			.mapToInt(pkg -> pkg.getClasses().size())
			.sum();
		out.printf("            <div class=\"summary-item\"><strong>Classes:</strong> %d</div>%n", totalClasses);
		out.println("        </div>");
	}

	private void writePackages(CoverageData data, PrintWriter out) {
		for (Map.Entry<String, PackageCoverage> entry : data.getPackages().entrySet()) {
			String pkgName = entry.getKey();
			PackageCoverage pkg = entry.getValue();
			
			out.println("        <div class=\"package\">");
			out.printf("            <div class=\"package-header\" onclick=\"togglePackage('%s')\">%n", 
				escapeHtml(pkgName).replace(".", "_"));
			out.printf("                <h2><span class=\"toggle-icon\" id=\"icon-%s\">▶</span>Package: %s %s</h2>%n",
				escapeHtml(pkgName).replace(".", "_"),
				escapeHtml(pkgName),
				getCoverageBadge(pkg.getPercentage()));
			out.printf("                <div>Coverage: %d / %d (%s)</div>%n",
				pkg.getImplementedCount(), pkg.getTotalCount(), formatPercentage(pkg.getPercentage()));
			writeProgressBar(out, pkg.getPercentage());
			out.println("            </div>");
			out.printf("            <div class=\"package-content\" id=\"pkg-%s\">%n", 
				escapeHtml(pkgName).replace(".", "_"));
			
			for (ClassCoverage cls : pkg.getClasses()) {
				writeClass(cls, out);
			}
			
			out.println("            </div>");
			out.println("        </div>");
		}
	}

	private void writeClass(ClassCoverage cls, PrintWriter out) {
		String classStyle = cls.getPercentage() == 100 ? "class" : 
			cls.getPercentage() > 50 ? "class partial" : "class missing";
		String safeClassName = escapeHtml(cls.getRuntimeClassName()).replace(".", "_").replace("$", "_");
		
		out.printf("                <div class=\"%s\">%n", classStyle);
		out.printf("                    <div class=\"class-header\" onclick=\"toggleClass('%s')\">%n", safeClassName);
		out.printf("                        <h3><span class=\"toggle-icon\" id=\"icon-%s\">▶</span>%s %s</h3>%n",
			safeClassName,
			escapeHtml(cls.getSimpleClassName()),
			getCoverageBadge(cls.getPercentage()));
		out.printf("                        <div>Coverage: %d / %d (%s)</div>%n",
			cls.getImplementedCount(), cls.getTotalCount(), formatPercentage(cls.getPercentage()));
		writeProgressBar(out, cls.getPercentage());
		out.println("                    </div>");
		out.printf("                    <div class=\"class-content\" id=\"cls-%s\">%n", safeClassName);
		
		writeClassDetails(cls, out);
		
		out.println("                    </div>");
		out.println("                </div>");
	}

	private void writeClassDetails(ClassCoverage cls, PrintWriter out) {
		String safeClassName = escapeHtml(cls.getRuntimeClassName()).replace(".", "_").replace("$", "_");
		
		if (!cls.getImplementedMethods().isEmpty()) {
			String sectionIdStr = safeClassName + "_impl_methods";
			out.println("                        <div class=\"section\">");
			out.printf("                            <div class=\"section-title collapsible\" onclick=\"toggleSection('%s')\">%n", sectionIdStr);
			out.printf("                                <span class=\"toggle-icon\" id=\"icon-%s\">▶</span> ✓ Implemented Methods (%d)%n", 
				sectionIdStr, cls.getImplementedMethods().size());
			out.println("                            </div>");
			out.printf("                            <ul class=\"item-list implemented\" id=\"%s\" style=\"display: none;\">%n", sectionIdStr);
			for (String method : cls.getImplementedMethods()) {
				out.printf("                                <li>%s</li>%n", escapeHtml(method));
			}
			out.println("                            </ul>");
			out.println("                        </div>");
		}
		
		if (!cls.getMissingMethods().isEmpty()) {
			String sectionIdStr = safeClassName + "_miss_methods";
			out.println("                        <div class=\"section\">");
			out.printf("                            <div class=\"section-title collapsible\" onclick=\"toggleSection('%s')\">%n", sectionIdStr);
			out.printf("                                <span class=\"toggle-icon\" id=\"icon-%s\">▶</span> ✗ Missing Methods (%d)%n", 
				sectionIdStr, cls.getMissingMethods().size());
			out.println("                            </div>");
			out.printf("                            <ul class=\"item-list missing\" id=\"%s\" style=\"display: none;\">%n", sectionIdStr);
			for (String method : cls.getMissingMethods()) {
				out.printf("                                <li>%s</li>%n", escapeHtml(method));
			}
			out.println("                            </ul>");
			out.println("                        </div>");
		}
		
		if (!cls.getImplementedFields().isEmpty()) {
			String sectionIdStr = safeClassName + "_impl_fields";
			out.println("                        <div class=\"section\">");
			out.printf("                            <div class=\"section-title collapsible\" onclick=\"toggleSection('%s')\">%n", sectionIdStr);
			out.printf("                                <span class=\"toggle-icon\" id=\"icon-%s\">▶</span> ✓ Implemented Fields (%d)%n", 
				sectionIdStr, cls.getImplementedFields().size());
			out.println("                            </div>");
			out.printf("                            <ul class=\"item-list implemented\" id=\"%s\" style=\"display: none;\">%n", sectionIdStr);
			for (String field : cls.getImplementedFields()) {
				out.printf("                                <li>%s</li>%n", escapeHtml(field));
			}
			out.println("                            </ul>");
			out.println("                        </div>");
		}
		
		if (!cls.getMissingFields().isEmpty()) {
			String sectionIdStr = safeClassName + "_miss_fields";
			out.println("                        <div class=\"section\">");
			out.printf("                            <div class=\"section-title collapsible\" onclick=\"toggleSection('%s')\">%n", sectionIdStr);
			out.printf("                                <span class=\"toggle-icon\" id=\"icon-%s\">▶</span> ✗ Missing Fields (%d)%n", 
				sectionIdStr, cls.getMissingFields().size());
			out.println("                            </div>");
			out.printf("                            <ul class=\"item-list missing\" id=\"%s\" style=\"display: none;\">%n", sectionIdStr);
			for (String field : cls.getMissingFields()) {
				out.printf("                                <li>%s</li>%n", escapeHtml(field));
			}
			out.println("                            </ul>");
			out.println("                        </div>");
		}
		
		if (!cls.getImplementedConstructors().isEmpty()) {
			String sectionIdStr = safeClassName + "_impl_ctors";
			out.println("                        <div class=\"section\">");
			out.printf("                            <div class=\"section-title collapsible\" onclick=\"toggleSection('%s')\">%n", sectionIdStr);
			out.printf("                                <span class=\"toggle-icon\" id=\"icon-%s\">▶</span> ✓ Implemented Constructors (%d)%n", 
				sectionIdStr, cls.getImplementedConstructors().size());
			out.println("                            </div>");
			out.printf("                            <ul class=\"item-list implemented\" id=\"%s\" style=\"display: none;\">%n", sectionIdStr);
			for (String ctor : cls.getImplementedConstructors()) {
				out.printf("                                <li>%s</li>%n", escapeHtml(ctor));
			}
			out.println("                            </ul>");
			out.println("                        </div>");
		}
		
		if (!cls.getMissingConstructors().isEmpty()) {
			String sectionIdStr = safeClassName + "_miss_ctors";
			out.println("                        <div class=\"section\">");
			out.printf("                            <div class=\"section-title collapsible\" onclick=\"toggleSection('%s')\">%n", sectionIdStr);
			out.printf("                                <span class=\"toggle-icon\" id=\"icon-%s\">▶</span> ✗ Missing Constructors (%d)%n", 
				sectionIdStr, cls.getMissingConstructors().size());
			out.println("                            </div>");
			out.printf("                            <ul class=\"item-list missing\" id=\"%s\" style=\"display: none;\">%n", sectionIdStr);
			for (String ctor : cls.getMissingConstructors()) {
				out.printf("                                <li>%s</li>%n", escapeHtml(ctor));
			}
			out.println("                            </ul>");
			out.println("                        </div>");
		}
	}

	private void writeProgressBar(PrintWriter out, double percentage) {
		String fillClass = percentage >= 75 ? "progress-fill" :
			percentage >= 50 ? "progress-fill medium" : "progress-fill low";
		out.println("                <div class=\"progress-bar\">");
		out.printf("                    <div class=\"%s\" style=\"width: %.1f%%\">%.1f%%</div>%n", 
			fillClass, percentage, percentage);
		out.println("                </div>");
	}

	private String getCoverageBadge(double percentage) {
		String badgeClass = percentage == 100 ? "full" :
			percentage >= 50 ? "partial" : "low";
		return String.format("<span class=\"coverage-badge %s\">%.1f%%</span>", badgeClass, percentage);
	}

	private String formatPercentage(double percentage) {
		return String.format("%.1f%%", percentage);
	}

	private String escapeHtml(String text) {
		if (text == null) return "";
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

	private void writeScript(PrintWriter out) {
		out.println("    <script>");
		out.println("        function togglePackage(pkgId) {");
		out.println("            const content = document.getElementById('pkg-' + pkgId);");
		out.println("            const icon = document.getElementById('icon-' + pkgId);");
		out.println("            if (content.classList.contains('expanded')) {");
		out.println("                content.classList.remove('expanded');");
		out.println("                icon.textContent = '▶';");
		out.println("            } else {");
		out.println("                content.classList.add('expanded');");
		out.println("                icon.textContent = '▼';");
		out.println("            }");
		out.println("        }");
		out.println("        function toggleClass(clsId) {");
		out.println("            const content = document.getElementById('cls-' + clsId);");
		out.println("            const icon = document.getElementById('icon-' + clsId);");
		out.println("            if (content.classList.contains('expanded')) {");
		out.println("                content.classList.remove('expanded');");
		out.println("                icon.textContent = '▶';");
		out.println("            } else {");
		out.println("                content.classList.add('expanded');");
		out.println("                icon.textContent = '▼';");
		out.println("            }");
		out.println("        }");
		out.println("        function toggleSection(sectionId) {");
		out.println("            const content = document.getElementById(sectionId);");
		out.println("            const icon = document.getElementById('icon-' + sectionId);");
		out.println("            if (content.style.display === 'none') {");
		out.println("                content.style.display = 'block';");
		out.println("                icon.textContent = '▼';");
		out.println("            } else {");
		out.println("                content.style.display = 'none';");
		out.println("                icon.textContent = '▶';");
		out.println("            }");
		out.println("        }");
		out.println("    </script>");
	}
}
