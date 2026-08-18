import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaseSummaryListener implements TestExecutionListener {

    private static final Pattern CASE_PATTERN = Pattern.compile("(API #TC\\s*\\d+)\\s*-\\s*(.+)");

    private static class SubCase {
        String label;
        boolean passed;
    }

    private static class Case {
        String id;
        String title;
        final List<SubCase> subCases = new ArrayList<>();

        boolean allPassed() {
            return subCases.stream().allMatch(s -> s.passed);
        }
    }

    private static class Category {
        String name;
        final Map<String, Case> cases = new LinkedHashMap<>();
    }

    private final Map<String, Category> categories = new LinkedHashMap<>();
    private TestPlan testPlan;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        if (!testIdentifier.isTest()) return;

        Optional<TestIdentifier> parentOpt = testPlan.getParent(testIdentifier);
        String parentDisplayName = parentOpt.map(TestIdentifier::getDisplayName).orElse("");

        Matcher own = CASE_PATTERN.matcher(testIdentifier.getDisplayName());
        Matcher parent = CASE_PATTERN.matcher(parentDisplayName);

        String caseId;
        String title;
        String subLabel;
        String categoryName;

        if (own.find()) {
            caseId = own.group(1).replace("API #TC", "TC").trim();
            title = own.group(2).trim();
            subLabel = null;
            categoryName = parentOpt.map(TestIdentifier::getDisplayName).orElse("Other");
        } else if (parent.find()) {
            caseId = parent.group(1).replace("API #TC", "TC").trim();
            title = parent.group(2).trim();
            subLabel = testIdentifier.getDisplayName();
            categoryName = parentOpt.flatMap(testPlan::getParent)
                    .map(TestIdentifier::getDisplayName).orElse("Other");
        } else {
            caseId = "TC??";
            title = testIdentifier.getDisplayName();
            subLabel = null;
            categoryName = "Other";
        }

        Category category = categories.computeIfAbsent(categoryName, n -> {
            Category c = new Category();
            c.name = n;
            return c;
        });

        Case c = category.cases.computeIfAbsent(caseId, id -> {
            Case nc = new Case();
            nc.id = id;
            nc.title = title;
            return nc;
        });

        SubCase sc = new SubCase();
        sc.label = subLabel;
        sc.passed = result.getStatus() == TestExecutionResult.Status.SUCCESSFUL;
        c.subCases.add(sc);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
            writeReport();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeReport() throws IOException {
        Path outDir = Paths.get("target", "case-report");
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("index.html");

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset='utf-8'><title>API Test Case Report</title><style>")
                .append("body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;background:#f7f7f8;color:#1a1a1a;margin:0;padding:24px;}")
                .append("h1{font-size:20px;margin-bottom:8px;} h2{font-size:16px;margin:24px 0 8px;}")
                .append("table{border-collapse:collapse;width:100%;background:#fff;border:1px solid #e2e2e5;border-radius:8px;overflow:hidden;margin-bottom:16px;}")
                .append("th,td{padding:8px 12px;text-align:left;border-bottom:1px solid #e2e2e5;font-size:13px;}")
                .append("tr:last-child td{border-bottom:none;}")
                .append("th{background:#f0f0f2;font-weight:600;}")
                .append(".case-row td{font-weight:600;}")
                .append(".sub-row td:first-child{padding-left:32px;color:#555;font-weight:400;}")
                .append(".badge{padding:2px 10px;border-radius:12px;font-size:12px;font-weight:600;}")
                .append(".pass{background:#e6f4ea;color:#1e7e34;} .fail{background:#fdecea;color:#c62828;}")
                .append(".summary td:not(:first-child),.summary th:not(:first-child){text-align:right;}")
                .append("</style></head><body><h1>API Test Case Report</h1>");

        int grandTotalCases = 0;
        int grandPassedCases = 0;
        List<String[]> summaryRows = new ArrayList<>();

        List<Category> sortedCategories = new ArrayList<>(categories.values());
        sortedCategories.sort(Comparator.comparing(cat -> cat.name));

        for (Category category : sortedCategories) {
            List<Case> sortedCases = new ArrayList<>(category.cases.values());
            sortedCases.sort(Comparator.comparing(c -> c.id));

            html.append("<h2>").append(escape(category.name)).append(" (").append(sortedCases.size()).append(" cases)</h2>");
            html.append("<table><thead><tr><th>Case</th><th>Status</th></tr></thead><tbody>");

            int catTotal = 0;
            int catPassed = 0;

            for (Case c : sortedCases) {
                catTotal++;
                boolean passed = c.allPassed();
                if (passed) catPassed++;

                html.append("<tr class='case-row'><td>").append(escape(c.id)).append(" - ").append(escape(c.title)).append("</td>")
                        .append("<td><span class='badge ").append(passed ? "pass" : "fail").append("'>")
                        .append(passed ? "PASS" : "FAIL").append("</span></td></tr>");

                boolean hasNamedSubs = c.subCases.stream().anyMatch(s -> s.label != null);
                if (hasNamedSubs) {
                    for (SubCase sc : c.subCases) {
                        html.append("<tr class='sub-row'><td>").append(escape(sc.label)).append("</td>")
                                .append("<td><span class='badge ").append(sc.passed ? "pass" : "fail").append("'>")
                                .append(sc.passed ? "PASS" : "FAIL").append("</span></td></tr>");
                    }
                }
            }
            html.append("</tbody></table>");

            grandTotalCases += catTotal;
            grandPassedCases += catPassed;
            summaryRows.add(new String[]{category.name, String.valueOf(catTotal), String.valueOf(catPassed),
                    String.valueOf(catTotal - catPassed), formatPct(catPassed, catTotal)});
        }

        html.append("<h2>Summary</h2>");
        html.append("<table class='summary'><thead><tr><th>Category</th><th>Total Cases</th><th>Passed</th><th>Failed</th><th>Pass %</th></tr></thead><tbody>");
        for (String[] row : summaryRows) {
            html.append("<tr><td>").append(escape(row[0])).append("</td><td>").append(row[1]).append("</td><td>")
                    .append(row[2]).append("</td><td>").append(row[3]).append("</td><td>").append(row[4]).append("</td></tr>");
        }
        html.append("<tr style='font-weight:700;'><td>Total</td><td>").append(grandTotalCases).append("</td><td>")
                .append(grandPassedCases).append("</td><td>").append(grandTotalCases - grandPassedCases).append("</td><td>")
                .append(formatPct(grandPassedCases, grandTotalCases)).append("</td></tr>");
        html.append("</tbody></table>");

        html.append("</body></html>");
        Files.write(outFile, html.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("[CaseSummaryListener] Report written to " + outFile.toAbsolutePath());
    }

    private static String formatPct(int passed, int total) {
        if (total == 0) return "0.0%";
        return String.format(java.util.Locale.US, "%.1f%%", passed * 100.0 / total);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
