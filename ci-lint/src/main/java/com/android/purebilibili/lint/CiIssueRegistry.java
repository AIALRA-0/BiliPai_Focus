package com.android.purebilibili.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.Vendor;
import com.android.tools.lint.detector.api.Issue;
import java.util.ArrayList;
import java.util.List;

import static com.android.tools.lint.detector.api.ApiKt.CURRENT_API;

/** Source policies are Lint diagnostics; runtime performance is tested separately. */
public final class CiIssueRegistry extends IssueRegistry {
    private static final List<Issue> ISSUES = issues();
    private static final Vendor VENDOR = new Vendor(
            "BiliPai", "com.android.purebilibili.lint",
            "https://github.com/jay3-yy/BiliPai/issues");

    private static List<Issue> issues() {
        List<Issue> issues = new ArrayList<>(StyleDetector.ISSUES);
        issues.add(PerformanceDetector.ISSUE);
        issues.add(ModelStabilityDetector.ISSUE);
        return List.copyOf(issues);
    }

    @Override
    public List<Issue> getIssues() {
        return ISSUES;
    }

    @Override
    public int getApi() {
        return CURRENT_API;
    }

    @Override
    public int getMinApi() {
        return CURRENT_API;
    }
    @Override
    public Vendor getVendor() {
        return VENDOR;
    }
}
